

package es.unex.sextante.hydrology.createExclusionAreas;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;


public class CreateExclusionAreasAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String RESULT       = "RESULT";
   public static final String OUTLET_POINT = "OUTLET_POINT";
   public static final String INPUT        = "INPUT";

   int                        m_iNX, m_iNY;
   IRasterLayer               m_Grid;
   boolean                    m_IsCellAlreadyVisited[][];
   private IRasterLayer       m_ExclusionAreas;


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("CreateExclusionAreas"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(false);

      try {
         m_Parameters.addInputRasterLayer(INPUT, Sextante.getText("Layer"), true);
         m_Parameters.addPoint(OUTLET_POINT, Sextante.getText("Outlet_point"));
         addOutputRasterLayer(RESULT, Sextante.getText("Result"), 0);
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_Grid = m_Parameters.getParameterValueAsRasterLayer(INPUT);

      m_Grid.setFullExtent();
      m_Grid.setInterpolationMethod(IRasterLayer.INTERPOLATION_NearestNeighbour);

      m_iNX = m_Grid.getNX();
      m_iNY = m_Grid.getNY();

      m_IsCellAlreadyVisited = new boolean[m_iNX][m_iNY];

      m_ExclusionAreas = getNewRasterLayer(RESULT, Sextante.getText("ExclusionAreas"), IRasterLayer.RASTER_DATA_TYPE_INT,
               m_Grid.getLayerGridExtent());

      m_ExclusionAreas.assign(m_Grid);
      m_ExclusionAreas.setNoDataValue(0.0);

      final Point2D pt = m_Parameters.getParameterValueAsPoint(OUTLET_POINT);
      final GridCell cell = m_Grid.getLayerGridExtent().getGridCoordsFromWorldCoords(pt);

      exclude(cell.getX(), cell.getY());

      m_ExclusionAreas.setNoData(cell.getX(), cell.getY());

      return !m_Task.isCanceled();

   }


   private void exclude(int x,
                        int y) {

      int x2, y2;
      int iInitClass;
      int iPt;
      int n;
      int iClass;
      double dArea = 0;
      ArrayList centralPoints = new ArrayList();
      ArrayList adjPoints = new ArrayList();
      Point point;

      iInitClass = m_Grid.getCellValueAsInt(x, y);

      centralPoints.add(new Point(x, y));
      m_IsCellAlreadyVisited[x][y] = true;

      while ((centralPoints.size() != 0) && !m_Task.isCanceled()) {
         for (iPt = 0; iPt < centralPoints.size(); iPt++) {
            dArea += m_Grid.getWindowCellSize() * m_Grid.getWindowCellSize();
            point = (Point) centralPoints.get(iPt);
            x = point.x;
            y = point.y;
            double dClass = m_Grid.getCellValueAsInt(x, y);
            if (!m_Grid.isNoDataValue(dClass)) {
               for (n = 0; n < 8; n++) {
                  x2 = x + m_iOffsetX[n];
                  y2 = y + m_iOffsetY[n];
                  dClass = m_Grid.getCellValueAsDouble(x2, y2);
                  if (!m_Grid.isNoDataValue(dClass)) {
                     iClass = (int) dClass;
                     if (m_IsCellAlreadyVisited[x2][y2] == false) {
                        if (iInitClass == iClass) {
                           m_IsCellAlreadyVisited[x2][y2] = true;
                           adjPoints.add(new Point(x2, y2));
                           m_ExclusionAreas.setNoData(x2, y2);
                        }
                     }
                  }
               }
            }
         }

         centralPoints = adjPoints;
         System.out.println(centralPoints.size());
         adjPoints = new ArrayList();

      }

      m_ExclusionAreas.setNoData(x, y);

   }

}
