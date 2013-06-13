package es.unex.sextante.hydrology.upslopeAreaFromArea;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridWrapper;

public class UpslopeAreaFromAreaAlgorithm
         extends
            GeoAlgorithm {

   private final static int    m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int    m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };
   private static final double NOT_VISITED  = 0.0;
   private static final double VISITED      = 1.0;

   public static final String  DEM          = "DEM";
   public static final String  INITZONES    = "INITZONES";
   public static final String  RESULT       = "RESULT";

   private int                 m_iNX, m_iNY;

   private IRasterLayer        m_DEM        = null;
   private IRasterLayer        m_InitZone   = null;
   private IRasterLayer        m_Result;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_InitZone = m_Parameters.getParameterValueAsRasterLayer(INITZONES);


      m_DEM.setFullExtent();
      final AnalysisExtent extent = m_DEM.getWindowGridExtent();
      m_InitZone.setWindowExtent(extent);
      m_InitZone.setInterpolationMethod(GridWrapper.INTERPOLATION_NearestNeighbour);

      m_Result = getNewRasterLayer(RESULT, Sextante.getText("Upslope_area"), IRasterLayer.RASTER_DATA_TYPE_BYTE, extent);
      m_Result.assign(NOT_VISITED);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateUpslopeArea();

      m_Result.setNoDataValue(NOT_VISITED);

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {


      setName(Sextante.getText("Upslope_area_from_outlet_zone"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(false);
      setIsDeterminatedProcess(false);

      try {
         m_Parameters.addInputRasterLayer(DEM,

         Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(INITZONES, Sextante.getText("Outlet_zone"), true);
         addOutputRasterLayer(RESULT, Sextante.getText("Upslope_area"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateUpslopeArea() {

      int x, y;
      double dValue;

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_InitZone.getCellValueAsDouble(x, y);
            if (!m_InitZone.isNoDataValue(dValue)) {
               calculateUpslopeAreaFromPoint(x, y);
            }
         }

      }

   }


   private void calculateUpslopeAreaFromPoint(final int x,
                                              final int y) {

      int i, ix, iy;
      double dValue;
      int iDirection;

      if ((m_Result.getCellValueAsDouble(x, y) != NOT_VISITED) || m_Task.isCanceled()) {
         return;
      }

      dValue = m_DEM.getCellValueAsDouble(x, y);
      if (m_DEM.isNoDataValue(dValue)) {
         return;
      }

      m_Result.setCellValue(x, y, VISITED);
      for (i = 0; i < 8; i++) {
         ix = x + m_iOffsetX[i];
         iy = y + m_iOffsetY[i];
         dValue = m_DEM.getCellValueAsDouble(ix, iy);
         if (!m_DEM.isNoDataValue(dValue)) {
            iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
            if (iDirection >= 0) {
               if ((i + 4) % 8 == iDirection) {
                  calculateUpslopeAreaFromPoint(ix, iy);
               }
            }
         }
      }

   }

}
