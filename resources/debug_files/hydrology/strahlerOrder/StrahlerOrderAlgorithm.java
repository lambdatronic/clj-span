package es.unex.sextante.hydrology.strahlerOrder;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class StrahlerOrderAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String RESULT       = "RESULT";

   private int                m_iNX, m_iNY;

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Strahler;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int x, y;

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);

      m_Strahler = getNewRasterLayer(RESULT, Sextante.getText("Strahler_Order"), IRasterLayer.RASTER_DATA_TYPE_INT);

      m_Strahler.assign(0.0);

      final AnalysisExtent extent = m_Strahler.getWindowGridExtent();

      m_DEM.setWindowExtent(extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            getStrahlerOrder(x, y);
         }
      }

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Strahler_Order"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         addOutputRasterLayer(RESULT, Sextante.getText("Strahler_Order"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void getStrahlerOrder(final int x,
                                 final int y) {

      int i;
      int ix, iy;
      int iDirection;
      int iMaxOrder = 1;
      int iOrder = 1;
      int iMaxOrderCells = 0;

      if (m_DEM.isNoDataValue(m_DEM.getCellValueAsDouble(x, y))) {
         return;
      }

      if (m_Strahler.getCellValueAsInt(x, y) == 0) {
         ;
         m_Strahler.setCellValue(x, y, iMaxOrder);
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
            if (iDirection == (i + 4) % 8) {
               getStrahlerOrder(ix, iy);
               iOrder = m_Strahler.getCellValueAsInt(ix, iy);
               if (iOrder > iMaxOrder) {
                  iMaxOrder = iOrder;
                  iMaxOrderCells = 1;
               }
               else if (iOrder == iMaxOrder) {
                  iMaxOrderCells++;
               }
            }
         }

         if (iMaxOrderCells > 1) {
            iMaxOrder++;
         }

         m_Strahler.setCellValue(x, y, iMaxOrder);

      }

      //return iMaxOrder;

   }

}
