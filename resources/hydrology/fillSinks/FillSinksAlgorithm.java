package es.unex.sextante.hydrology.fillSinks;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class FillSinksAlgorithm
         extends
            GeoAlgorithm {

   private final static int    m_iOffsetX[]   = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int    m_iOffsetY[]   = { 1, 1, 0, -1, -1, -1, 0, 1 };
   private final static double INIT_ELEVATION = 50000D;
   public static final String  DEM            = "DEM";
   public static final String  MINSLOPE       = "MINSLOPE";
   public static final String  RESULT         = "RESULT";

   private int                 depth;
   private int                 m_iNX, m_iNY;
   private final double        dEpsilon[]     = new double[8];
   private int                 R, C;
   private final int[]         R0             = new int[8];
   private final int[]         C0             = new int[8];
   private final int[]         dR             = new int[8];
   private final int[]         dC             = new int[8];
   private final int[]         fR             = new int[8];
   private final int[]         fC             = new int[8];
   private IRasterLayer        m_DEM          = null;
   private IRasterLayer        m_Border;
   private IRasterLayer        m_PreprocessedDEM;


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Sink_filling"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      super.setUserCanDefineAnalysisExtent(false);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addNumericalValue(MINSLOPE, Sextante.getText("Min_angle_between_cells_[degrees]"), 0.01,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(RESULT, Sextante.getText("Preprocessed"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int i;
      int iValue;
      int x, y;
      int ix, iy;
      int scan;
      int it;
      double z, z2, wz, wzn;
      double dMinSlope;
      boolean something_done = false;

      depth = 0;

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      dMinSlope = Math.tan(Math.toRadians(m_Parameters.getParameterValueAsDouble(MINSLOPE)));

      final AnalysisExtent ge = new AnalysisExtent(m_DEM);
      m_PreprocessedDEM = getNewRasterLayer(RESULT, m_DEM.getName() + Sextante.getText("[preprocessed]"),
               IRasterLayer.RASTER_DATA_TYPE_DOUBLE, ge);
      m_Border = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_INT, ge);

      m_DEM.setFullExtent();

      m_iNX = ge.getNX();
      m_iNY = ge.getNY();

      for (i = 0; i < 8; i++) {
         dEpsilon[i] = dMinSlope * m_DEM.getDistToNeighborInDir(i);
      }

      R0[0] = 0;
      R0[1] = m_iNY - 1;
      R0[2] = 0;
      R0[3] = m_iNY - 1;
      R0[4] = 0;
      R0[5] = m_iNY - 1;
      R0[6] = 0;
      R0[7] = m_iNY - 1;
      C0[0] = 0;
      C0[1] = m_iNX - 1;
      C0[2] = m_iNX - 1;
      C0[3] = 0;
      C0[4] = m_iNX - 1;
      C0[5] = 0;
      C0[6] = 0;
      C0[7] = m_iNX - 1;
      dR[0] = 0;
      dR[1] = 0;
      dR[2] = 1;
      dR[3] = -1;
      dR[4] = 0;
      dR[5] = 0;
      dR[6] = 1;
      dR[7] = -1;
      dC[0] = 1;
      dC[1] = -1;
      dC[2] = 0;
      dC[3] = 0;
      dC[4] = -1;
      dC[5] = 1;
      dC[6] = 0;
      dC[7] = 0;
      fR[0] = 1;
      fR[1] = -1;
      fR[2] = -m_iNY + 1;
      fR[3] = m_iNY - 1;
      fR[4] = 1;
      fR[5] = -1;
      fR[6] = -m_iNY + 1;
      fR[7] = m_iNY - 1;
      fC[0] = -m_iNX + 1;
      fC[1] = m_iNX - 1;
      fC[2] = -1;
      fC[3] = 1;
      fC[4] = m_iNX - 1;
      fC[5] = -m_iNX + 1;
      fC[6] = 1;
      fC[7] = -1;

      initAltitude();

      setProgressText("Fase 1");
      for (x = 0; (x < m_iNX) && setProgress(x, m_iNX); x++) {
         for (y = 0; y < m_iNY; y++) {
            iValue = m_Border.getCellValueAsInt(x, y);
            if (iValue == 1) {
               dryUpwardCell(x, y);
            }
         }

      }

      if (m_Task.isCanceled()) {
         return false;
      }

      for (it = 0; it < 1000; it++) {
         setProgressText("fase 2. Iteracion " + Integer.toString(it));
         for (scan = 0; scan < 8; scan++) {
            R = R0[scan];
            C = C0[scan];
            something_done = false;
            if (!setProgress(scan, 8)) {
               return false;
            }
            do {
               z = m_DEM.getCellValueAsDouble(C, R);
               wz = m_PreprocessedDEM.getCellValueAsDouble(C, R);
               if (!m_DEM.isNoDataValue(z) && (wz > z)) {
                  for (i = 0; i < 8; i++) {
                     ix = C + m_iOffsetX[i];
                     iy = R + m_iOffsetY[i];
                     z2 = m_DEM.getCellValueAsDouble(ix, iy);
                     if (!m_DEM.isNoDataValue(z2)) {
                        wzn = m_PreprocessedDEM.getCellValueAsDouble(ix, iy) + dEpsilon[i];
                        if (z >= wzn) {
                           m_PreprocessedDEM.setCellValue(C, R, z);
                           something_done = true;
                           dryUpwardCell(C, R);
                           break;
                        }
                        if (wz > wzn) {
                           m_PreprocessedDEM.setCellValue(C, R, wzn);
                           something_done = true;
                        }
                     }
                  }
               }
            }
            while (nextCell(scan));

            if (!something_done) {
               break;
            }
         }
         if (!something_done) {
            break;
         }
      }

      return !m_Task.isCanceled();

   }


   private boolean nextCell(final int i) {

      R = R + dR[i];
      C = C + dC[i];

      if ((R < 0) || (C < 0) || (R >= m_iNY) || (C >= m_iNX)) {
         R = R + fR[i];
         C = C + fC[i];

         if ((R < 0) || (C < 0) || (R >= m_iNY) || (C >= m_iNX)) {
            return false;
         }
      }

      return true;
   }


   private void dryUpwardCell(final int x,
                              final int y) {

      final int MAX_DEPTH = 32000;
      int ix, iy, i;
      double zn, zw;

      depth += 1;

      if (depth <= MAX_DEPTH) {
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            zw = m_PreprocessedDEM.getCellValueAsDouble(ix, iy);
            zn = m_DEM.getCellValueAsDouble(ix, iy);
            if (!m_DEM.isNoDataValue(zn) && (zw == INIT_ELEVATION)) {
               zw = m_PreprocessedDEM.getCellValueAsDouble(x, y) + dEpsilon[i];
               if (zn >= zw) {
                  m_PreprocessedDEM.setCellValue(ix, iy, zn);
                  dryUpwardCell(ix, iy);
               }
            }
         }
      }
      depth -= 1;

   }


   private void initAltitude() {

      boolean border;
      int x, y, i, ix, iy;
      double dValue;

      m_Border.assignNoData();
      m_PreprocessedDEM.assignNoData();

      for (x = 0; x < m_iNX; x++) {
         for (y = 0; y < m_iNY; y++) {
            border = false;
            dValue = m_DEM.getCellValueAsDouble(x, y);
            if (!m_DEM.isNoDataValue(dValue)) {
               for (i = 0; i < 8; i++) {
                  ix = x + m_iOffsetX[i];
                  iy = y + m_iOffsetY[i];
                  dValue = m_DEM.getCellValueAsDouble(ix, iy);
                  if (m_DEM.isNoDataValue(dValue)) {
                     border = true;
                     break;
                  }
               }
               if (border) {
                  m_Border.setCellValue(x, y, 1);
                  m_PreprocessedDEM.setCellValue(x, y, m_DEM.getCellValueAsDouble(x, y));
               }
               else {
                  m_PreprocessedDEM.setCellValue(x, y, INIT_ELEVATION);
               }
            }
         }
      }

   }


}
