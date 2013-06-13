

package es.unex.sextante.hydrology.upslopeAreaFromPoint;

import java.awt.geom.Point2D;

import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;


public class UpslopeAreaFromPointAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String OUTLET       = "OUTLET";
   public static final String RESULT       = "RESULT";

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Watershed;
   private GridCell           m_Outlet;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      final Point2D pt = m_Parameters.getParameterValueAsPoint(OUTLET);

      final AnalysisExtent gridExtent = new AnalysisExtent(m_DEM);
      m_DEM.setFullExtent();
      m_Watershed = getNewRasterLayer(RESULT, Sextante.getText("Upslope_area"), IRasterLayer.RASTER_DATA_TYPE_BYTE, gridExtent);

      m_Watershed.assign(0.0);

      m_Outlet = gridExtent.getGridCoordsFromWorldCoords(pt);

      calculateWatershed();

      m_Watershed.setNoDataValue(0.0);

      return true;

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Upslope_area_from_a_single_point"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(false);
      setIsDeterminatedProcess(false);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addPoint(OUTLET, Sextante.getText("Outlet_point"));
         addOutputRasterLayer(RESULT, Sextante.getText("Upslope_area"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateWatershed() {

      writeCell(m_Outlet.getX(), m_Outlet.getY());

   }


   private void writeCell(final int iX,
                          final int iY) {

      int i;
      int ix, iy;
      int iDirection;
      double dValue;

      dValue = m_DEM.getCellValueAsDouble(iX, iY);

      if (!m_DEM.isNoDataValue(dValue)) {

         m_Watershed.setCellValue(iX, iY, 1);

         for (i = 0; i < 8; i++) {
            ix = iX + m_iOffsetX[i];
            iy = iY + m_iOffsetY[i];
            dValue = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(dValue)) {
               iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy, false);
               if (iDirection >= 0) {
                  if ((i + 4) % 8 == iDirection) {
                     writeCell(ix, iy);
                  }
               }
            }
         }

      }

   }
}
