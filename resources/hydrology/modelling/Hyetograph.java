package es.unex.sextante.hydrology.modelling;

import java.awt.geom.Point2D;

public class Hyetograph {

   private double  m_dRainfall[];   // in mm
   private final int     m_iTimeInterval; // in seconds
   private Point2D m_Coords;
   private String  m_sName;


   public Hyetograph(final double dP24,
                     final int iTime, // in seconds
                     final int iTimeInterval,
                     final int iPeakTime,
                     final int iReturn,
                     final int iRatioI1) {

      int i;

      final int iLength = (int) ((double) iTime / (double) iTimeInterval);
      final double dRain[] = new double[iLength];
      double dAccRain = 0;
      double dTotalRain = 0;

      for (i = 0; i < iLength; i++) {
         dTotalRain = getIntensity((double) (iTimeInterval * (i + 1)) / 3600.0, dP24, iRatioI1)
                      * (double) (iTimeInterval * (i + 1)) / 3600.0;
         dRain[i] = dTotalRain - dAccRain;
         dAccRain = dTotalRain;
      }

      m_iTimeInterval = iTimeInterval;

      final int iPeakInterval = (int) ((float) iPeakTime / (float) iTimeInterval) - 1;
      final int iMaxIntervals = Math.max(iPeakInterval, iLength - 1 - iPeakInterval);
      m_dRainfall = new double[iLength];
      int iInterval = 0;
      int iRainInterval = 0;

      m_dRainfall[iPeakInterval] = dRain[0];
      for (i = 1; i < iMaxIntervals + 1; i++) {
         iInterval = iPeakInterval + i;
         if (iInterval < iLength) {
            iRainInterval++;
            m_dRainfall[iInterval] = dRain[iRainInterval];
         }
         iInterval = iPeakInterval - i;
         if (iInterval >= 0) {
            iRainInterval++;
            m_dRainfall[iInterval] = dRain[iRainInterval];
         }
      }


   }


   public Hyetograph(final double[] dRainfall,
                     final int iTimeInterval) {

      int i;

      m_dRainfall = new double[dRainfall.length];

      for (i = 0; i < dRainfall.length; i++) {
         m_dRainfall[i] = dRainfall[i];
      }

      m_iTimeInterval = iTimeInterval;

   }


   public Hyetograph(final double dP05,// in mm
                     final double dP1,
                     final double dP6) {

      double dDelta;

      dDelta = dP6 - dP1;
      m_dRainfall = new double[12];
      m_dRainfall[0] = dDelta * 0.15f / 2f;
      m_dRainfall[1] = dDelta * 0.15f / 2f;
      m_dRainfall[2] = dDelta * 0.17f / 2f;
      m_dRainfall[3] = dDelta * 0.17f / 2f;
      m_dRainfall[4] = dDelta * 0.19f / 2f;
      m_dRainfall[5] = dDelta * 0.19f / 2f;
      m_dRainfall[6] = dP05;
      m_dRainfall[7] = dP1 - dP05;
      m_dRainfall[8] = dDelta * 0.32f / 2f;
      m_dRainfall[9] = dDelta * 0.32f / 2f;
      m_dRainfall[10] = dDelta * 0.17f / 2f;
      m_dRainfall[11] = dDelta * 0.17f / 2f;
      m_iTimeInterval = 1800;

   }


   public int getDuration() {

      return m_dRainfall.length * m_iTimeInterval;

   }


   public int getIntervals() {

      return m_dRainfall.length;

   }


   public void delay(final int iTime) {//in seconds

      final int iDelayIntervals = (int) ((double) iTime / (double) m_iTimeInterval);
      final double dRainfall[] = new double[m_dRainfall.length + iDelayIntervals];

      for (int i = 0; i < m_dRainfall.length; i++) {
         dRainfall[i + iDelayIntervals] = m_dRainfall[i];
      }
      m_dRainfall = dRainfall;

   }


   public int getTimeInterval() {

      return m_iTimeInterval;

   }


   public double[] getRainfallArray() {

      return m_dRainfall;

   }


   public double getAverageRainfallIntensity() {// in mm/s

      double dAverage = 0;

      for (int i = 0; i < m_dRainfall.length; i++) {
         dAverage = dAverage + m_dRainfall[i];
      }
      dAverage = dAverage / (m_dRainfall.length * (float) m_iTimeInterval);

      return dAverage;

   }


   public double getIntensity(// in mm/s
                              final double dInitTime,// in s
                              final double dDuration) {// in s

      return (getRainfall(dInitTime, dDuration) / dDuration);

   }


   public double getRainfall(// in mm
                             final double dInitTime,// in s
                             final double dDuration) {// in s

      double dTotal = 0;

      int iInitInterval = (int) Math.floor((dInitTime / (double) m_iTimeInterval));
      int iEndInterval = (int) Math.floor(((dInitTime + dDuration) / (double) m_iTimeInterval));
      iInitInterval = Math.max(iInitInterval, 0);
      iEndInterval = Math.min(iEndInterval, m_dRainfall.length - 1);

      for (int i = iInitInterval; i < iEndInterval + 1; i++) {
         dTotal += m_dRainfall[i];
      }
      dTotal -= m_dRainfall[iInitInterval] * ((dInitTime / (double) m_iTimeInterval) - iInitInterval);
      dTotal -= m_dRainfall[iEndInterval] * (1 - (((dInitTime + dDuration) / (double) m_iTimeInterval) - iEndInterval));

      return dTotal;

   }


   public double getTotalRainfall() {

      double dTotal = 0;

      for (int i = 0; i < m_dRainfall.length; i++) {
         dTotal += m_dRainfall[i];
      }

      return dTotal;

   }


   public void normalize() {

      final double dTotal = getTotalRainfall();

      for (int i = 0; i < m_dRainfall.length; i++) {
         m_dRainfall[i] /= dTotal;
      }

   }


   public Hyetograph getNormalized() {

      final Hyetograph hyet = new Hyetograph(m_dRainfall, m_iTimeInterval);

      hyet.normalize();

      return hyet;

   }


   private double getIntensity(// mm/h
                               final double dTime, // in h
                               final double dP24,
                               final int iRatioI1) {

      final double dI = (double) ((double) dP24 / 24.0 * Math.pow((double) iRatioI1, (Math.pow(28.0, 0.1) - Math.pow(dTime, 0.1))
                                                                               / (Math.pow(28.0, 0.1) - 1.0)));

      return dI;

   }


   public void setCoords(final Point2D coords) {

      m_Coords = coords;

   }


   public Point2D getCoords() {

      return m_Coords;

   }


   public void setName(final String sName) {

      m_sName = sName;

   }


   public String getName() {

      return m_sName;

   }


   public double getRainfallAtInterval(final int iInterval) {

      if (iInterval < m_dRainfall.length && iInterval > 0) {
         return m_dRainfall[iInterval];
      }
      else {
         return 0;
      }

   }

}
