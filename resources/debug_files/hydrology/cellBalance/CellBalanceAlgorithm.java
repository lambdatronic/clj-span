package es.unex.sextante.hydrology.cellBalance;


import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class CellBalanceAlgorithm
         extends
            GeoAlgorithm {

   private static final double RAD_TO_DEG   = 180.0 / Math.PI;
   private final static int    m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int    m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String  METHOD       = "METHOD";
   public static final String  DEM          = "DEM";
   public static final String  WEIGHTS      = "WEIGHTS";
   public static final String  CONVERGENCE  = "CONVERGENCE";
   public static final String  CELLBALANCE  = "CELLBALANCE";

   private int                 m_iMethod;
   private int                 m_iNX, m_iNY;
   private double              m_dConvergence;

   private IRasterLayer        m_DEM        = null;
   private IRasterLayer        m_Weights    = null;
   private IRasterLayer        m_CellBalance;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_iMethod = m_Parameters.getParameterValueAsInt(METHOD);
      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Weights = m_Parameters.getParameterValueAsRasterLayer(WEIGHTS);
      m_dConvergence = m_Parameters.getParameterValueAsDouble(CONVERGENCE);

      m_CellBalance = getNewRasterLayer(CELLBALANCE, Sextante.getText("Net_balance"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);

      final AnalysisExtent extent = m_CellBalance.getWindowGridExtent();
      m_DEM.setWindowExtent(extent);
      m_Weights.setWindowExtent(extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateInFlows();
      substractOutFlows();

      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      final String[] sMethod = { Sextante.getText("D8"), Sextante.getText("DInfinity"),
               Sextante.getText("MFD__Multiple_Flow_Directions") };

      setName(Sextante.getText("Net_balance"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer("WEIGHTS", Sextante.getText("Weight"), true);
         m_Parameters.addSelection(METHOD, Sextante.getText("Method"), sMethod);
         m_Parameters.addNumericalValue(CONVERGENCE, Sextante.getText("Convergence_factor__MFD"), 1.1,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(CELLBALANCE, Sextante.getText("Net_balance"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateInFlows() {

      int x, y;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            switch (m_iMethod) {
               case 0:
                  doD8(x, y);
                  break;
               case 1:
                  doDInf(x, y);
                  break;
               case 2:
                  doMFD(x, y);
                  break;
            }
         }
      }

   }


   private void substractOutFlows() {

      int x, y;
      double dOut;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            dOut = m_Weights.getCellValueAsDouble(x, y);
            if (!m_Weights.isNoDataValue(dOut)) {
               m_CellBalance.addToCellValue(x, y, -dOut);
            }
            else {
               m_CellBalance.setNoData(x, y);
            }
         }
      }

   }


   private void doD8(final int x,
                     final int y) {

      int iDirection;
      double dWeight;
      int ix, iy;

      dWeight = m_Weights.getCellValueAsDouble(x, y);
      if (!m_Weights.isNoDataValue(dWeight)) {
         iDirection = m_DEM.getDirToNextDownslopeCell(x, y);
         if (iDirection >= 0) {
            ix = x + m_iOffsetX[iDirection];
            iy = y + m_iOffsetY[iDirection];
            m_CellBalance.addToCellValue(ix, iy, dWeight);
         }
      }

   }


   private void doDInf(final int x,
                       final int y) {

      int iDirection;
      double dWeight;
      double dAspect;
      int ix, iy;

      dWeight = m_Weights.getCellValueAsDouble(x, y);
      if (!m_Weights.isNoDataValue(dWeight)) {
         dAspect = m_DEM.getAspect(x, y);
         dAspect *= RAD_TO_DEG;
         if (dAspect >= 0) {
            iDirection = (int) (dAspect / 45.0);
            dAspect = (dAspect % 45) / 45.0;

            iDirection = iDirection % 8;
            ix = x + m_iOffsetX[iDirection];
            iy = y + m_iOffsetY[iDirection];
            m_CellBalance.addToCellValue(ix, iy, dWeight * (1.0 - dAspect));

            iDirection = (iDirection + 1) % 8;
            ix = x + m_iOffsetX[iDirection];
            iy = y + m_iOffsetY[iDirection];
            m_CellBalance.addToCellValue(ix, iy, dWeight * dAspect);
         }
      }

   }


   private void doMFD(final int x,
                      final int y) {

      int i, ix, iy;
      double z, z2, dDifZ, dzSum;
      double Flow[];
      double dWeight;

      dWeight = m_Weights.getCellValueAsDouble(x, y);
      if (!m_Weights.isNoDataValue(dWeight)) {
         z = m_DEM.getCellValueAsDouble(x, y);
         dzSum = 0.0;
         Flow = new double[8];
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            z2 = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(z2)) {
               dDifZ = z - z2;
               if (dDifZ > 0) {
                  dzSum += Flow[i] = Math.pow(dDifZ / m_DEM.getDistToNeighborInDir(i), m_dConvergence);
               }
            }
         }

         if (dzSum > 0.0) {
            for (i = 0; i < 8; i++) {
               ix = x + m_iOffsetX[i];
               iy = y + m_iOffsetY[i];
               if (Flow[i] > 0) {
                  m_CellBalance.addToCellValue(ix, iy, Flow[i] / dzSum * dWeight);
               }
            }
         }
      }
   }

}
