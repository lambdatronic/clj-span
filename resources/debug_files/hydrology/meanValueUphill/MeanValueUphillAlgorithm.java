package es.unex.sextante.hydrology.meanValueUphill;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class MeanValueUphillAlgorithm
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
   private IRasterLayer        m_AccFlow;
   private IRasterLayer        m_MeanValue;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int x, y;
      int iCells;
      double dValue;

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Param = m_Parameters.getParameterValueAsRasterLayer(PARAM);

      m_MeanValue = getNewRasterLayer(RESULT, Sextante.getText("Mean_value_uphill"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);
      m_AccFlow = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_INT, m_MeanValue.getWindowGridExtent());

      m_MeanValue.assign(NOT_VISITED);
      m_AccFlow.assign(NOT_VISITED);

      final AnalysisExtent extent = m_MeanValue.getWindowGridExtent();

      m_DEM.setWindowExtent(extent);
      m_Param.setWindowExtent(extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateAccFlows();

      m_MeanValue.setNoDataValue(NOT_VISITED);

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_MeanValue.getCellValueAsDouble(x, y);
            iCells = m_AccFlow.getCellValueAsInt(x, y);
            if (!m_MeanValue.isNoDataValue(dValue) && (iCells != 0)) {
               m_MeanValue.setCellValue(x, y, dValue / iCells);
            }
            else {
               m_MeanValue.setNoData(x, y);
            }
         }
      }


      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Mean_value_uphill"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(PARAM, Sextante.getText("Parameter"), true);
         addOutputRasterLayer(RESULT, Sextante.getText("Mean_value_uphill"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateAccFlows() {

      int x, y;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            getFlow(x, y);
         }

      }

   }


   private void getFlow(final int x,
                        final int y) {

      int i, ix, iy;
      int iDirection;
      double dValue;
      double dParamValue;

      if (m_AccFlow.getCellValueAsDouble(x, y) != NOT_VISITED) {
         return;
      }

      dParamValue = m_Param.getCellValueAsDouble(x, y);
      dValue = m_DEM.getCellValueAsDouble(x, y);
      if (!m_DEM.isNoDataValue(dValue) /*&& !m_Param.isNoDataValue(dParamValue)*/) {
         if (!m_Param.isNoDataValue(dParamValue)) {
            m_AccFlow.setCellValue(x, y, 1);
            m_MeanValue.setCellValue(x, y, dParamValue);
         }
         else {
            m_AccFlow.setCellValue(x, y, 0);
            m_MeanValue.setCellValue(x, y, 0);
         }
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            dValue = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(dValue)) {
               iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
               if (iDirection >= 0) {
                  if ((i + 4) % 8 == iDirection) {
                     getFlow(ix, iy);
                     //dParamValue = m_Param.getCellValueAsDouble(ix, iy);
                     //if (!m_Param.isNoDataValue(dParamValue)){
                     m_AccFlow.addToCellValue(x, y, m_AccFlow.getCellValueAsDouble(ix, iy));
                     m_MeanValue.addToCellValue(x, y, m_MeanValue.getCellValueAsDouble(ix, iy));
                     //}
                  }
               }
            }
         }
      }


   }

}
