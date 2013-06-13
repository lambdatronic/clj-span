package es.unex.sextante.hydrology.maxValueUphill;


import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class MaxValueUphillAlgorithm
         extends
            GeoAlgorithm {

   private final static int    m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int    m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };
   private static final double NOT_VISITED  = -1.0;

   public static final String  DEM          = "DEM";
   public static final String  PARAM        = "PARAM";
   public static final String  RESULT       = "RESULT";

   private int                 m_iNX, m_iNY;
   private IRasterLayer        m_DEM        = null;
   private IRasterLayer        m_Param      = null;
   private IRasterLayer        m_MaxValue;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Param = m_Parameters.getParameterValueAsRasterLayer(PARAM);

      m_MaxValue = getNewRasterLayer(RESULT, Sextante.getText("Max_value_uphill"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);


      m_MaxValue.assign(NOT_VISITED);

      final AnalysisExtent extent = m_MaxValue.getWindowGridExtent();

      m_DEM.setWindowExtent(extent);
      m_Param.setWindowExtent(extent);

      m_MaxValue.setNoDataValue(m_Param.getNoDataValue());

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateMaxValues();

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Max_value_uphill"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(PARAM, Sextante.getText("Parameter"), true);
         addOutputRasterLayer(RESULT, Sextante.getText("Max_value_uphill"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateMaxValues() {

      int x, y;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            getMaxValue(x, y);
         }

      }

   }


   private void getMaxValue(final int x,
                            final int y) {

      int i, ix, iy;
      int iDirection;
      double dValue;
      double dParamValue;
      double dMaxValue;


      if (m_Task.isCanceled()) {
         return;
      }


      if (m_MaxValue.getCellValueAsDouble(x, y) != NOT_VISITED) {
         return;
      }

      dValue = m_DEM.getCellValueAsDouble(x, y);
      if (!m_DEM.isNoDataValue(dValue)) {
         dParamValue = m_Param.getCellValueAsDouble(x, y);
         if (!m_Param.isNoDataValue(dParamValue)) {
            m_MaxValue.setCellValue(x, y, dParamValue);
         }
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            dValue = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(dValue)) {
               iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
               if (iDirection >= 0) {
                  if ((i + 4) % 8 == iDirection) {
                     getMaxValue(ix, iy);
                     dMaxValue = m_MaxValue.getCellValueAsDouble(x, y);
                     if (m_MaxValue.isNoDataValue(dMaxValue)) {
                        m_MaxValue.setCellValue(x, y, m_MaxValue.getCellValueAsDouble(ix, iy));
                     }
                     else {
                        m_MaxValue.setCellValue(x, y, Math.max(m_MaxValue.getCellValueAsDouble(x, y),
                                 m_MaxValue.getCellValueAsDouble(ix, iy)));
                     }
                  }
               }
            }
         }
      }
      else {
         m_MaxValue.setNoData(x, y);
      }


   }

}
