package es.unex.sextante.hydrology.isocrones;

import java.awt.geom.Point2D;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;

public class IsocronesAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String NETWORK      = "NETWORK";
   public static final String OUTLET       = "OUTLET";
   public static final String TIME         = "TIME";
   public static final String RATIO        = "RATIO";

   private int                m_iNX, m_iNY;
   private double             m_dSpeed;
   private double             m_dRatio;
   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Network    = null;
   private IRasterLayer       m_TimeOut;
   private GridCell           m_Outlet;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Network = m_Parameters.getParameterValueAsRasterLayer(NETWORK);
      final Point2D pt = m_Parameters.getParameterValueAsPoint(OUTLET);

      final AnalysisExtent gridExtent = new AnalysisExtent(m_DEM);
      m_DEM.setWindowExtent(gridExtent);
      m_TimeOut = getNewRasterLayer(TIME, Sextante.getText("Time_to_outlet__h"), IRasterLayer.RASTER_DATA_TYPE_FLOAT, gridExtent);

      m_TimeOut.assign(0.0);

      m_Outlet = gridExtent.getGridCoordsFromWorldCoords(pt);

      if (m_Network != null) {
         m_Network.setWindowExtent(gridExtent);
      }

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      m_dSpeed = 1.0;
      m_dRatio = 1.0;
      calculateTimeOfConcentration();

      m_dRatio = m_Parameters.getParameterValueAsDouble(RATIO);
      calculateTimeOut();

      m_TimeOut.setNoDataValue(0.0);

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Time_to_outlet"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(false);
      setIsDeterminatedProcess(false);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(NETWORK, Sextante.getText("Channel_network"), false);
         m_Parameters.addNumericalValue(RATIO, Sextante.getText("speed_ratio__channel_-_overland"), 10,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         m_Parameters.addPoint(OUTLET, Sextante.getText("Outlet_point"));
         addOutputRasterLayer(TIME, Sextante.getText("Time_to_outlet"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateTimeOfConcentration() {

      int x, y;
      double dValue;
      final GridCell highestCell = new GridCell(0, 0, Double.NEGATIVE_INFINITY);

      writeTimeOut(m_Outlet.getX(), m_Outlet.getY(), m_Outlet.getX(), m_Outlet.getY());

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_TimeOut.getCellValueAsDouble(x, y);
            if (!m_TimeOut.isNoDataValue(dValue)) {
               if (dValue > highestCell.getValue()) {
                  highestCell.setX(x);
                  highestCell.setY(y);
                  highestCell.setValue(dValue);
               }
            }
         }
      }

      final double dH1 = m_DEM.getCellValueAsDouble(m_Outlet.getX(), m_Outlet.getY());
      final double dH2 = m_DEM.getCellValueAsDouble(highestCell.getX(), highestCell.getY());
      final double dConcTime = Math.pow(0.87 * Math.pow(highestCell.getValue() / 1000., 3) / (dH2 - dH1), 0.385);
      m_dSpeed = highestCell.getValue() / dConcTime;

   }


   private void calculateTimeOut() {

      m_TimeOut.assign(0.0);

      writeTimeOut(m_Outlet.getX(), m_Outlet.getY(), m_Outlet.getX(), m_Outlet.getY());

   }


   private void writeTimeOut(final int iX1,
                             final int iY1,
                             final int iX2,
                             final int iY2) {

      int i;
      int ix, iy;
      int iDirection;
      double dDist = 1;
      double dTime;
      double dValue;


      if (m_Task.isCanceled()) {
         return;
      }

      dValue = m_DEM.getCellValueAsDouble(iX1, iY1);

      if (!m_DEM.isNoDataValue(dValue)) {
         if ((iX1 == iX2) && (iY1 == iY2)) {
            dDist = 0;
         }
         else if (Math.abs(iX1 - iX2 + iY1 - iY2) == 1) {
            dDist = m_DEM.getDistToNeighborInDir(0);
         }
         else {
            dDist = m_DEM.getDistToNeighborInDir(1);
         }
         dTime = dDist / m_dSpeed;

         if (m_Network != null) {
            dValue = m_Network.getCellValueAsDouble(iX1, iY1);
            if (m_Network.isNoDataValue(dValue) || (dValue == 0)) {
               dTime *= m_dRatio;
            }
         }

         dTime += m_TimeOut.getCellValueAsDouble(iX2, iY2);
         m_TimeOut.setCellValue(iX1, iY1, dTime);

         for (i = 0; i < 8; i++) {
            ix = iX1 + m_iOffsetX[i];
            iy = iY1 + m_iOffsetY[i];
            dValue = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(dValue)) {
               iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
               if (iDirection >= 0) {
                  if ((i + 4) % 8 == iDirection) {
                     writeTimeOut(ix, iy, iX1, iY1);
                  }
               }
            }
         }

      }

   }
}
