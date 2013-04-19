package es.unex.sextante.hydrology.modelling;

public class Hydrograph {

   protected double m_dFlow[];       // in m3
   protected int    m_iTimeInterval; // in seconds
   private String   m_sName;


   public Hydrograph(final int iInterval) {

      m_dFlow = new double[1];
      m_iTimeInterval = iInterval;

   }


   public Hydrograph(final double[] H,
                     final int iInterval) {

      m_dFlow = H;
      m_iTimeInterval = iInterval;

   }


   public void delay(final int iTime) {//in seconds

      int i;
      final int iDelayIntervals = (int) ((double) iTime / (double) m_iTimeInterval);
      final double dFlow[] = new double[m_dFlow.length + iDelayIntervals];

      for (i = 0; i < iDelayIntervals + 1; i++) {
         dFlow[i] = 0;
      }
      for (i = 0; i < m_dFlow.length; i++) {
         dFlow[i + iDelayIntervals] = m_dFlow[i];
      }
      m_dFlow = dFlow;

   }


   public void add(final Hydrograph Hyd) {

      double dFlow1 = 0;
      double dFlow2 = 0;

      if (Hyd.getTimeInterval() == m_iTimeInterval) {
         final int iLength = Math.max(Hyd.getLengthInIntervals(), m_dFlow.length);
         final double dFlow[] = new double[iLength];

         for (int i = 0; i < iLength; i++) {
            if (i >= Hyd.getLengthInIntervals()) {
               dFlow1 = 0;
            }
            else {
               dFlow1 = Hyd.getFlow(m_iTimeInterval * i);
            }
            if (i >= m_dFlow.length) {
               dFlow2 = 0;
            }
            else {
               dFlow2 = m_dFlow[i];
            }
            dFlow[i] = dFlow1 + dFlow2;
         }
         m_dFlow = dFlow;
      }

   }


   public void add(final float fFlow) {

      for (int i = 0; i < m_dFlow.length; i++) {
         m_dFlow[i] += fFlow;
      }

   }


   /*public void MuskingumRouting(
           float fK,
           float fX) {

       ArrayList RoutedHydrograph = new ArrayList();
       int j = 0;
       float fFlow1 = 0;
       float fFlow0 = 0;
       float fRoutedFlow = 0;

       float fD = m_iTimeInterval / 3600f;
       int iReaches = 1 + (int) (fK * 2f * fX * fD);

       fK /= (float) iReaches;
       for (int i = 0; i < iReaches; i++) {
           float fC3 = fK - fK * fX + 0.5f * fD;
           float fC0 = (-fK * fX + 0.5f * fD) / fC3;
           float fC1 = (fK * fX + 0.5f * fD) / fC3;
           float fC2 = (fK - fK * fX - 0.5f * fD) / fC3;

           j = 0;
           boolean bIn = false;

           fRoutedFlow = m_fFlow[0];
           RoutedHydrograph.clear();
           RoutedHydrograph.add(new Float(fRoutedFlow));
           do {
               j++;
               if (j < m_fFlow.length) {
                   fFlow1 = m_fFlow[j];
               }// if
               else {
                   fFlow1 = 0;
                   if (!bIn) {
                       break;// exit if Hydrogram is null//
                   }// if
               }// else
               if (j - 1 < m_fFlow.length) {
                   fFlow0 = m_fFlow[j - 1];
               }// if
               else {
                   fFlow0 = 0;
               }// else
               fRoutedFlow = Math.max(0,
                       fC0 * fFlow1 + fC1 * fFlow0 + fC2 * fRoutedFlow);
               RoutedHydrograph.add(new Float(fRoutedFlow));
               if (fRoutedFlow > 0.25f) {
                   bIn = true;
               }// if
           }while (fRoutedFlow > 0.25f || !bIn);
           m_fFlow = new float[RoutedHydrograph.size()];
           for (int k = 0; k < RoutedHydrograph.size(); k++) {
               m_fFlow[k] = ((Float) RoutedHydrograph.get(k)).floatValue();
           }// for
       }// for

   }// method*/

   public double getFlow(final int iTime) {

      final int iIndex = (int) ((double) iTime / (double) m_iTimeInterval);

      return m_dFlow[iIndex];

   }


   public int getTimeInterval() {

      return m_iTimeInterval;

   }


   public int getLengthInIntervals() {

      return m_dFlow.length;

   }


   public double getTotalRunoff() {// im m3

      double dRunoff = 0;

      for (int i = 0; i < m_dFlow.length - 1; i++) {
         dRunoff += (m_dFlow[i] + m_dFlow[i + 1]) / 2.0 * m_iTimeInterval;
      }

      return dRunoff;

   }


   public double getPeak() {// in m3

      double dMax = Double.NEGATIVE_INFINITY;

      for (int i = 0; i < m_dFlow.length; i++) {
         if (m_dFlow[i] > dMax) {
            dMax = m_dFlow[i];
         }
      }

      return dMax;

   }


   public int getPeakTime() {// in seconds

      double dMax = Double.NEGATIVE_INFINITY;
      int iMaxTime = 0;

      for (int i = 0; i < m_dFlow.length; i++) {
         if (m_dFlow[i] > dMax) {
            dMax = m_dFlow[i];
            iMaxTime = i * m_iTimeInterval;
         }
      }

      return iMaxTime;

   }


   public double[] getFlowArray() {

      return m_dFlow;

   }


   /*public Pt[] getPtArray() {

       Pt PtArray[] = new Pt[m_fFlow.length];

       for (int i = 0; i < m_fFlow.length; i++) {
           PtArray[i] = new Pt((float) (m_iTimeInterval * i), m_fFlow[i]);
       }// for

       return PtArray;

   }// method*/

   public void multiply(final double fFactor) {

      for (int i = 0; i < m_dFlow.length; i++) {
         m_dFlow[i] *= fFactor;
      }

   }


   @Override
   public String toString() {

      final StringBuffer sb = new StringBuffer();

      sb.append("Caudal Punta : " + getPeak() + " m3/s\n");
      sb.append("Tiempo al Pico : " + transformSecToHourMin(getPeakTime()) + " \n");
      sb.append("Volumen total de escorrentía : " + getTotalRunoff() + " m3\n");
      sb.append("Duración total del hidrograma : " + transformSecToHourMin(getLengthInIntervals() * m_iTimeInterval) + "\n");

      return sb.toString();

   }


   public static String transformSecToHourMin(final int iSec) {

      final int iHours = (int) ((double) iSec / 3600f);
      final int iMin = (int) ((double) (iSec - iHours * 3600) / 60.0);

      return new String(Integer.toString(iHours) + "h" + Integer.toString(iMin) + "min");

   }


   public void setName(final String sName) {

      m_sName = sName;

   }


   public String getName() {

      return m_sName;

   }


}
