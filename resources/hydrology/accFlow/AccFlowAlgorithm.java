package es.unex.sextante.hydrology.accFlow;


import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class AccFlowAlgorithm
         extends
            GeoAlgorithm {

   private static final double NOT_VISITED  = -1.0;

   public static final String  FLOWACC      = "FLOWACC";
   public static final String  CONVERGENCE  = "CONVERGENCE";
   public static final String  METHOD       = "METHOD";
   public static final String  DEM          = "DEM";
   public static final String  WEIGHTS      = "WEIGHTS";

   public final static int     D8           = 0;
   public final static int     RHO8         = 1;
   public final static int     DINF         = 2;
   public final static int     MFD          = 3;

   private final static int    m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int    m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   private int                 m_iMethod;
   private int                 m_iNX, m_iNY;
   private IRasterLayer        m_Flow[];
   private double              m_dConvergence;

   private IRasterLayer        m_DEM        = null;
   private IRasterLayer        m_Weights    = null;
   private IRasterLayer        m_AccFlow;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int i;

      m_iMethod = m_Parameters.getParameterValueAsInt(METHOD);
      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Weights = m_Parameters.getParameterValueAsRasterLayer(WEIGHTS);
      m_dConvergence = m_Parameters.getParameterValueAsDouble(CONVERGENCE);

      m_AccFlow = getNewRasterLayer(FLOWACC, Sextante.getText("Flow_accumulation"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);

      m_AccFlow.assign(NOT_VISITED);
      final AnalysisExtent extent = m_AccFlow.getWindowGridExtent();

      m_DEM.setWindowExtent(extent);
      if (m_Weights != null) {
         m_Weights.setWindowExtent(extent);
      }

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      m_Flow = new IRasterLayer[8];

      for (i = 0; i < m_Flow.length; i++) {
         m_Flow[i] = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_FLOAT, extent);
      }

      calculateFlows();
      calculateAccFlows();

      if (m_Task.isCanceled()) {
         return false;
      }
      else {
         m_AccFlow.multiply(m_AccFlow.getWindowCellSize() * m_AccFlow.getWindowCellSize());
         return true;
      }

   }


   @Override
   public void defineCharacteristics() {

      final String[] sMethod = { Sextante.getText("D8"), Sextante.getText("Rho8"), Sextante.getText("DInfinity"),
               Sextante.getText("MFD__Multiple_Flow_Directions") };

      setName(Sextante.getText("Flow_accumulation"));

      setUserCanDefineAnalysisExtent(true);
      setGroup(Sextante.getText("Basic_hydrological_analysis"));

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(WEIGHTS, Sextante.getText("Weight"), false);
         m_Parameters.addSelection(METHOD, Sextante.getText("Method"), sMethod);
         m_Parameters.addNumericalValue(CONVERGENCE, Sextante.getText("Convergence_factor__MFD"), 1.1,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(FLOWACC, Sextante.getText("Flow_accumulation"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateFlows() {

      int x, y;

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            switch (m_iMethod) {
               case 0:
                  doD8(x, y);
                  break;
               case 1:
                  doRho8(x, y);
                  break;
               case 2:
                  doDInf(x, y);
                  break;
               case 3:
                  doMFD(x, y);
                  break;
            }
         }
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

      int i, ix, iy, j;
      double dFlow;
      double dValue, dValue2;
      double dWeight;

      if ((m_AccFlow.getCellValueAsDouble(x, y) != NOT_VISITED) || m_Task.isCanceled()) {
         return;
      }

      if (m_Weights != null) {
         dWeight = m_Weights.getCellValueAsDouble(x, y);
      }
      else {
         dWeight = 1.0;
      }

      dValue = m_DEM.getCellValueAsDouble(x, y);
      if (!m_DEM.isNoDataValue(dValue)) {
         m_AccFlow.setCellValue(x, y, dWeight);
         for (i = 0, j = 4; i < 8; i++, j = (j + 1) % 8) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            dValue = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(dValue)) {
               dFlow = m_Flow[j].getCellValueAsDouble(ix, iy);
               if (dFlow > 0) {
                  getFlow(ix, iy);
                  dValue = m_AccFlow.getCellValueAsDouble(ix, iy) * dFlow;
                  dValue2 = m_AccFlow.getCellValueAsDouble(x, y);
                  m_AccFlow.setCellValue(x, y, dValue + dValue2);
               }
            }
         }
      }


   }


   private void doD8(final int x,
                     final int y) {

      int iDirection;

      iDirection = m_DEM.getDirToNextDownslopeCell(x, y);

      if (iDirection >= 0) {
         m_Flow[iDirection % 8].setCellValue(x, y, 1.0f);
      }

   }


   private void doRho8(final int x,
                       final int y) {

      int iDirection;
      double dAspect;

      dAspect = Math.toDegrees(m_DEM.getAspect(x, y));

      if (dAspect >= 0) {
         iDirection = (int) (dAspect / 45.0);

         if ((dAspect % 45.0) / 45.0 > Math.random()) {
            iDirection++;
         }

         iDirection %= 8;

         m_Flow[iDirection].setCellValue(x, y, 1.0f);
      }

   }


   private void doDInf(final int x,
                       final int y) {

      int iDirection;
      double dAspect;

      dAspect = Math.toDegrees(m_DEM.getAspect(x, y));

      if (dAspect >= 0) {
         iDirection = (int) (dAspect / 45.0);
         dAspect = (dAspect % 45) / 45.0;

         m_Flow[iDirection % 8].setCellValue(x, y, 1.0 - dAspect);
         m_Flow[(iDirection + 1) % 8].setCellValue(x, y, dAspect);
      }

   }


   private void doMFD(final int x,
                      final int y) {

      int i, ix, iy;
      double z, z2, dDifZ, dzSum;
      double dFlow;

      z = m_DEM.getCellValueAsDouble(x, y);
      dzSum = 0.0;

      for (i = 0; i < 8; i++) {
         ix = x + m_iOffsetX[i];
         iy = y + m_iOffsetY[i];
         z2 = m_DEM.getCellValueAsDouble(ix, iy);
         if (!m_DEM.isNoDataValue(z2)) {
            dDifZ = z - z2;
            if (dDifZ > 0) {
               dFlow = Math.pow(dDifZ / m_DEM.getDistToNeighborInDir(i), m_dConvergence);
               dzSum += dFlow;
               m_Flow[i].setCellValue(x, y, dFlow);
            }
         }
      }

      if (dzSum > 0.0) {
         for (i = 0; i < 8; i++) {
            dFlow = m_Flow[i].getCellValueAsDouble(x, y);
            if (dFlow > 0) {
               m_Flow[i].setCellValue(x, y, dFlow / dzSum);
            }
         }
      }

   }

}
