

package es.unex.sextante.hydrology.burnStreams;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;


public class BurnStreamsAlgorithm
         extends
            GeoAlgorithm {

   public static final String  STREAMS          = "STREAMS";
   public static final String  DEM              = "DEM";
   public static final String  RESULT           = "RESULT";
   public static final String  DEPTH            = "DEPTH";

   private static final double SMALL_DIFFERENCE = 0.05;

   private IRasterLayer        m_DEM;
   private IRasterLayer        m_Result;
   private double              m_dLastZ;
   private double              m_dDepth;
   private boolean             m_bFoundValidCell;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_dDepth = m_Parameters.getParameterValueAsDouble(DEPTH);

      final IVectorLayer lines = m_Parameters.getParameterValueAsVectorLayer(STREAMS);

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_DEM.setFullExtent();
      final AnalysisExtent extent = m_DEM.getWindowGridExtent();

      m_Result = getNewRasterLayer(RESULT, Sextante.getText("Modified_DEM"), IRasterLayer.RASTER_DATA_TYPE_DOUBLE, extent);

      m_Result.assign(m_DEM);

      int iShape = 0;
      final int iShapeCount = lines.getShapesCount();
      final IFeatureIterator iterator = lines.iterator();
      while (iterator.hasNext() && setProgress(iShape, iShapeCount)) {
         final Geometry line = iterator.next().getGeometry();
         for (int i = 0; i < line.getNumGeometries(); i++) {
            processLine(line.getGeometryN(i));
         }
         iShape++;
      }
      iterator.close();

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Burn_streams"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(false);
      try {
         m_Parameters.addInputVectorLayer(STREAMS, Sextante.getText("Channel_network"),
                  AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addNumericalValue(DEPTH, Sextante.getText("Depth"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE,
                  10, 0, Double.MAX_VALUE);
         addOutputRasterLayer(RESULT, Sextante.getText("Modified_DEM"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void processLine(final Geometry line) {

      double x, y, x2, y2;
      final Coordinate[] coords = line.getCoordinates();

      m_bFoundValidCell = false;
      for (int i = 0; i < coords.length - 1; i++) {
         x = coords[i].x;
         y = coords[i].y;
         x2 = coords[i + 1].x;
         y2 = coords[i + 1].y;
         processSegment(x, y, x2, y2);
      }

   }


   private void processSegment(double x,
                               double y,
                               final double x2,
                               final double y2) {


      double dx, dy, d, n;

      dx = Math.abs(x2 - x);
      dy = Math.abs(y2 - y);

      if ((dx > 0.0) || (dy > 0.0)) {
         if (dx > dy) {
            dx /= m_DEM.getWindowCellSize();
            n = dx;
            dy /= dx;
            dx = m_DEM.getWindowCellSize();
         }
         else {
            dy /= m_DEM.getWindowCellSize();
            n = dy;
            dx /= dy;
            dy = m_DEM.getWindowCellSize();
         }

         if (x2 < x) {
            dx = -dx;
         }

         if (y2 < y) {
            dy = -dy;
         }

         for (d = 0.0; d <= n; d++, x += dx, y += dy) {
            addPoint(x, y);
         }
      }

   }


   private void addPoint(final double x,
                         final double y) {


      final GridCell cell = m_DEM.getWindowGridExtent().getGridCoordsFromWorldCoords(x, y);
      final int iX = cell.getX();
      final int iY = cell.getY();
      final double z = m_DEM.getCellValueAsDouble(iX, iY) /*- m_dDepth*/;
      if (m_DEM.isNoDataValue(z)) {
         return;
      }
      if (!m_bFoundValidCell) {
         m_dLastZ = z - m_dDepth;
         m_bFoundValidCell = true;
      }
      //if (z >= m_dLastZ) {
      m_dLastZ -= SMALL_DIFFERENCE;
      //}

      m_Result.setCellValue(iX, iY, m_dLastZ);

   }
}
