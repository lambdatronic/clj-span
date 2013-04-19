package es.unex.sextante.hydrology.watersheds;

import java.util.ArrayList;
import java.util.Arrays;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;

public class WatershedsAlgorithm
         extends
            GeoAlgorithm {

   private static final int   NO_BASIN     = -1;
   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String NETWORK      = "NETWORK";
   public static final String MINSIZE      = "MINSIZE";
   public static final String WATERSHEDS   = "WATERSHEDS";

   private int                m_iBasins;
   private int                m_iNX, m_iNY;
   private int                m_iMinSize;

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Network    = null;
   private IRasterLayer       m_Basins;
   private IRasterLayer       m_Directions;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Network = m_Parameters.getParameterValueAsRasterLayer(NETWORK);
      m_iMinSize = m_Parameters.getParameterValueAsInt(MINSIZE);

      m_Basins = getNewRasterLayer(WATERSHEDS, Sextante.getText("Watersheds"), IRasterLayer.RASTER_DATA_TYPE_INT);

      m_Basins.setNoDataValue(NO_BASIN);
      m_Basins.assignNoData();

      final AnalysisExtent extent = m_Basins.getWindowGridExtent();
      m_DEM.setFullExtent();
      m_Network.setWindowExtent(extent);

      m_Directions = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_INT, extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateBasins();

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Watersheds"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(NETWORK, Sextante.getText("Channel_network"), true);
         m_Parameters.addNumericalValue(MINSIZE, Sextante.getText("Minimum_watershed_size__cells"), 0,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
         addOutputRasterLayer(WATERSHEDS, Sextante.getText("Watersheds"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateBasins() {

      int i;
      int x, y;
      int iBasins;
      prepareDirectionsLayer();
      final ArrayList outletsArrayList = getOutlets();
      final Object[] outlets = outletsArrayList.toArray();
      Arrays.sort(outlets);

      m_iBasins = 0;
      for (i = outlets.length - 1; (i >= 0) && setProgress(outlets.length - i, outlets.length); i--) {
         m_iBasins++;
         x = ((GridCell) outlets[i]).getX();
         y = ((GridCell) outlets[i]).getY();
         if (getBasin(x, y) < m_iMinSize) {
            iBasins = m_iBasins - 1;
            m_iBasins = NO_BASIN;
            getBasin(x, y);
            m_iBasins = iBasins;
         }
      }

   }


   private void prepareDirectionsLayer() {

      int x, y;
      int iDir;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            iDir = m_DEM.getDirToNextDownslopeCell(x, y, false);
            if (iDir < 0) {
               m_Directions.setCellValue(x, y, -1.0);
            }
            else {
               m_Directions.setCellValue(x, y, ((iDir + 4) % 8));
            }
         }
      }

   }


   private ArrayList getOutlets() {

      int x, y;
      final ArrayList outlets = new ArrayList();

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            addOutlet(x, y, outlets);
         }

      }

      return outlets;

   }


   private void addOutlet(final int x,
                          final int y,
                          final ArrayList outlets) {

      int i;
      int ix, iy;
      int iUpslopeX = 0, iUpslopeY = 0;
      int iUpslopeRiverCells = 0;
      double dUpslopeZ = 0;
      int iNetwork = m_Network.getCellValueAsInt(x, y);
      final int iDir = m_Directions.getCellValueAsInt(x, y);
      final double dZ = m_DEM.getCellValueAsDouble(x, y);
      final int iNextCellX = x + m_iOffsetX[(iDir + 4) % 8];
      final int iNextCellY = y + m_iOffsetY[(iDir + 4) % 8];
      final int iNextCellNetwork = m_Network.getCellValueAsInt(iNextCellX, iNextCellY);

      if (m_Network.isNoDataValue(iNetwork) || (iNetwork == 0)) { //not a river cell
         return;
      }
      if (m_Network.isNoDataValue(iNextCellNetwork) || (iNextCellNetwork == 0)) { //border river cell
         outlets.add(new GridCell(x, y, dZ));
      }
      else if (iNetwork < 0) { // user defined outlet
         outlets.add(new GridCell(x, y, dZ));
         return;
      }
      else if ((iDir == -1) && !m_DEM.isNoDataValue(dZ)) { //border cell, limits with no data or with grid boundaries
         outlets.add(new GridCell(x, y, dZ));
      }
      else {
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            if (m_Directions.getCellValueAsInt(ix, iy) == i) {
               iNetwork = m_Network.getCellValueAsInt(ix, iy);
               if ((iNetwork > 0) && !m_Network.isNoDataValue(iNetwork)) {
                  if (iUpslopeRiverCells > 0) {
                     if (iUpslopeRiverCells == 1) {
                        outlets.add(new GridCell(iUpslopeX, iUpslopeY, dUpslopeZ));
                     }
                     outlets.add(new GridCell(ix, iy, m_DEM.getCellValueAsDouble(ix, iy)));
                  }
                  else {
                     iUpslopeX = ix;
                     iUpslopeY = iy;
                     dUpslopeZ = m_DEM.getCellValueAsDouble(x, y);
                  }
                  iUpslopeRiverCells++;
               }
            }
         }
      }

   }


   private int getBasin(final int x,
                        final int y) {

      int i, ix, iy, nCells = 1;

      final int iBasin = m_Basins.getCellValueAsInt(x, y);
      final int iDir = m_Directions.getCellValueAsInt(x, y);
      if ((iBasin == NO_BASIN) && !m_Directions.isNoDataValue(iDir)) {

         m_Basins.setCellValue(x, y, m_iBasins);

         for (i = 0, nCells = 1; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            if ((m_Directions.getCellValueAsInt(ix, iy) == i) && (m_Basins.getCellValueAsInt(ix, iy) == NO_BASIN)) {
               nCells += getBasin(ix, iy);
            }
         }
         return nCells;
      }

      return 0;
   }

}
