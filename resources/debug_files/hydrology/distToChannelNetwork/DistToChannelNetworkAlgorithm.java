package es.unex.sextante.hydrology.distToChannelNetwork;

import java.util.ArrayList;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;
import es.unex.sextante.rasterWrappers.GridWrapper;

public class DistToChannelNetworkAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String NETWORK      = "NETWORK";
   public static final String DIST         = "DIST";

   private int                m_iNX, m_iNY;

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Network    = null;
   private IRasterLayer       m_Dist;
   private IRasterLayer       m_Directions;
   private ArrayList          m_AdjPoints;
   private ArrayList          m_CentralPoints;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Network = m_Parameters.getParameterValueAsRasterLayer(NETWORK);

      m_Dist = getNewRasterLayer(DIST, Sextante.getText("Distance_to_channel_network"), IRasterLayer.RASTER_DATA_TYPE_DOUBLE);

      m_Dist.setNoDataValue(-1.0);
      m_Dist.assignNoData();

      final AnalysisExtent extent = m_Dist.getWindowGridExtent();
      m_DEM.setWindowExtent(extent);
      m_Network.setWindowExtent(extent);
      m_Network.setInterpolationMethod(GridWrapper.INTERPOLATION_NearestNeighbour);

      m_Directions = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_INT, extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      prepareInitData();

      calculateDistance();

      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Distance_to_channel_network"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);
      setIsDeterminatedProcess(false);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(NETWORK, Sextante.getText("Channel_network"), true);
         addOutputRasterLayer(DIST, Sextante.getText("Distance_to_channel_network"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void prepareInitData() {

      int x, y;
      int iDir;
      int iChannel;

      m_AdjPoints = new ArrayList();
      m_CentralPoints = new ArrayList();

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            iDir = m_DEM.getDirToNextDownslopeCell(x, y, false);
            iChannel = m_Network.getCellValueAsInt(x, y);
            if (iDir < 0) {
               m_Directions.setCellValue(x, y, -1.0);
            }
            else {
               m_Directions.setCellValue(x, y, ((iDir + 4) % 8));
            }
            if ((iChannel != 0) && !m_Network.isNoDataValue(iChannel)) {
               m_Dist.setCellValue(x, y, 0.0);
               m_CentralPoints.add(new GridCell(x, y, 0));
            }
         }
      }

   }


   private void calculateDistance() {

      int i;
      int iPt;
      int x, y, x2, y2;
      double dDist, dDist2, dAccDist;
      GridCell cell;

      while (m_CentralPoints.size() != 0) {
         for (iPt = 0; iPt < m_CentralPoints.size(); iPt++) {
            cell = (GridCell) m_CentralPoints.get(iPt);
            x = cell.getX();
            y = cell.getY();
            dDist = m_Dist.getCellValueAsDouble(x, y);
            for (i = 0; i < 8; i++) {
               x2 = x + m_iOffsetX[i];
               y2 = y + m_iOffsetY[i];
               if (m_Directions.getCellValueAsInt(x2, y2) == i) {
                  dAccDist = dDist + m_DEM.getDistToNeighborInDir(i);
                  dDist2 = m_Dist.getCellValueAsDouble(x2, y2);
                  if (m_Dist.isNoDataValue(dDist2) || (dDist2 > dAccDist)) {
                     m_Dist.setCellValue(x2, y2, dAccDist);
                     m_AdjPoints.add(new GridCell(x2, y2, 0));
                  }
               }
            }
         }

         m_CentralPoints = m_AdjPoints;
         m_AdjPoints = new ArrayList();

         if (m_Task.isCanceled()) {
            return;
         }

      }

   }

}
