package es.unex.sextante.hydrology.channelNetwork;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.outputs.OutputVectorLayer;
import es.unex.sextante.rasterWrappers.GridCell;

public class ChannelNetworkAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[]   = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[]   = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String METHOD         = "METHOD";
   public static final String DEM            = "DEM";
   public static final String THRESHOLDLAYER = "THRESHOLDLAYER";
   public static final String THRESHOLD      = "THRESHOLD";
   public static final String NETWORK        = "NETWORK";
   public static final String NETWORKVECT    = "NETWORKVECT";

   private int                m_iMethod;
   private int                m_iNX, m_iNY;
   private double             m_dThreshold;

   private IRasterLayer       m_DEM          = null;
   private IRasterLayer       m_Threshold    = null;
   private IRasterLayer       m_Network;

   private ArrayList          m_HeadersAndJunctions;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_iMethod = m_Parameters.getParameterValueAsInt(METHOD);
      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Threshold = m_Parameters.getParameterValueAsRasterLayer(THRESHOLDLAYER);
      m_dThreshold = m_Parameters.getParameterValueAsDouble(THRESHOLD);

      m_Network = getNewRasterLayer(NETWORK, Sextante.getText("Channel_network"), IRasterLayer.RASTER_DATA_TYPE_INT);

      m_Network.assign(0.0);

      final AnalysisExtent extent = m_Network.getWindowGridExtent();
      m_DEM.setWindowExtent(extent);
      m_Threshold.setWindowExtent(extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      calculateChannelNetwork();

      m_Network.setNoDataValue(0.0);


      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      final String sMethod[] = { Sextante.getText("Greater_than"), Sextante.getText("Lower_than") };

      setName(Sextante.getText("Channel_network"));
      setGroup(Sextante.getText("Basic_hydrological_analysis"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(THRESHOLDLAYER, Sextante.getText("Threshold_layer"), true);
         m_Parameters.addSelection(METHOD, Sextante.getText("Criteria"), sMethod);
         m_Parameters.addNumericalValue(THRESHOLD, Sextante.getText("Threshold"), 10000,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(NETWORK, Sextante.getText("Channel_network"));
         addOutputVectorLayer(NETWORKVECT, Sextante.getText("Channel_network"), OutputVectorLayer.SHAPE_TYPE_LINE);
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateChannelNetwork() throws GeoAlgorithmExecutionException {

      int i;
      final ArrayList alHeaders = getHeaders();

      if (alHeaders != null) {
         m_HeadersAndJunctions = alHeaders;
         final Object[] headers = alHeaders.toArray();
         Arrays.sort(headers);

         setProgressText(Sextante.getText("Delineating_channel_network"));
         if (headers.length > 0) {
            for (i = 0; (i < headers.length) && setProgress(i, headers.length); i++) {
               traceChannel((GridCell) headers[i]);
            }
         }

         if (!m_Task.isCanceled()) {
            calculateOrderAndAddJunctions();
            createVectorLayer();
         }
      }

   }


   private ArrayList getHeaders() {

      int iDirection;
      int x, y;
      int ix, iy;
      double dValue;
      double dHeight1, dHeight2;
      boolean bIsHeader;
      final ArrayList headers = new ArrayList();

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_Threshold.getCellValueAsDouble(x, y);
            dHeight1 = m_DEM.getCellValueAsDouble(x, y);
            if (meetsChannelConditions(dValue)) {
               bIsHeader = true;
               dHeight1 = m_DEM.getCellValueAsDouble(x, y);
               if (!m_DEM.isNoDataValue(dHeight1)) {
                  for (iDirection = 0; iDirection < 8; iDirection++) {
                     ix = x + m_iOffsetX[iDirection];
                     iy = y + m_iOffsetY[iDirection];
                     dValue = m_Threshold.getCellValueAsDouble(ix, iy);
                     if (meetsChannelConditions(dValue)) {
                        dHeight2 = m_DEM.getCellValueAsDouble(ix, iy);
                        if (dHeight2 >= dHeight1) {
                           bIsHeader = false;
                           break;
                        }
                     }
                  }
                  if (bIsHeader) {
                     headers.add(new GridCell(x, y, m_DEM.getCellValueAsDouble(x, y)));
                  }
               }
            }
         }
      }

      if (m_Task.isCanceled()) {
         return null;
      }
      else {
         return headers;
      }

   }


   private boolean meetsChannelConditions(final double dValue) {

      if (m_iMethod == 0) {
         return (dValue > m_dThreshold);
      }
      else if (m_iMethod == 1) {
         return (dValue < m_dThreshold);
      }

      return false;

   }


   private void traceChannel(final GridCell cell) {

      int iDirection;
      int x, y;
      boolean bContinue = true;

      x = cell.getX();
      y = cell.getY();

      do {
         m_Network.setCellValue(x, y, -1);
         iDirection = m_DEM.getDirToNextDownslopeCell(x, y);
         if (iDirection >= 0) {
            x = x + m_iOffsetX[iDirection];
            y = y + m_iOffsetY[iDirection];
         }
         else {
            bContinue = false;
         }
      }
      while (bContinue && !m_Task.isCanceled());

   }


   private void calculateOrderAndAddJunctions() {

      int x, y;

      setProgressText(Sextante.getText("Calculating_orders"));

      for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
         for (x = 0; x < m_iNX; x++) {
            getStrahlerOrder(x, y);
         }
      }

   }


   private int getStrahlerOrder(final int x,
                                final int y) {

      int i;
      int ix, iy;
      int iDirection;
      int iMaxOrder = 1;
      int iOrder = 1;
      int iMaxOrderCells = 0;
      int iUpslopeChannelCells = 0;

      if (m_Network.getCellValueAsInt(x, y) == -1) {
         ;
         for (i = 0; i < 8; i++) {
            ix = x + m_iOffsetX[i];
            iy = y + m_iOffsetY[i];
            iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
            if ((iDirection == (i + 4) % 8) && ((iOrder = m_Network.getCellValueAsInt(ix, iy)) != 0)) {
               iUpslopeChannelCells++;
               iOrder = m_Network.getCellValueAsInt(ix, iy);
               if (iOrder == -1) {
                  iOrder = getStrahlerOrder(ix, iy);
               }
               if (iOrder > iMaxOrder) {
                  iMaxOrder = iOrder;
                  iMaxOrderCells = 1;
               }
               else if (iOrder == iMaxOrder) {
                  iMaxOrderCells++;
               }
            }
         }

         if (iMaxOrderCells > 1) {
            iMaxOrder++;
         }

         if (iUpslopeChannelCells > 1) {
            m_HeadersAndJunctions.add(new GridCell(x, y, m_DEM.getCellValueAsDouble(x, y)));
         }

         m_Network.setCellValue(x, y, iMaxOrder);

      }

      return iMaxOrder;

   }


   private void createVectorLayer() throws GeoAlgorithmExecutionException {

      int i;
      int x, y;
      int ix, iy;
      int iDirection;
      int iIndexDownslope = -1;
      int iOrder;
      boolean bContinue;
      double dLength;
      Point2D pt;
      GridCell cell;
      ArrayList coordsList;
      final AnalysisExtent extent = m_DEM.getWindowGridExtent();
      final Object[] values = new Object[4];

      final String sNames[] = { Sextante.getText("ID"), Sextante.getText("Length"), Sextante.getText("Order"),
               Sextante.getText("Next") };
      final Class[] types = { Integer.class, Double.class, Integer.class, Integer.class };

      final IVectorLayer network = getNewVectorLayer(NETWORKVECT, Sextante.getText("Channel_network"),
               IVectorLayer.SHAPE_TYPE_LINE, types, sNames);
      final Object[] headers = m_HeadersAndJunctions.toArray();
      Arrays.sort(headers);

      setProgressText(Sextante.getText("Creating_vector_layer"));
      for (i = headers.length - 1; (i > -1) && setProgress(headers.length - i, headers.length); i--) {
         cell = (GridCell) headers[i];
         x = cell.getX();
         y = cell.getY();
         coordsList = new ArrayList();
         pt = extent.getWorldCoordsFromGridCoords(cell);
         coordsList.add(new Coordinate(pt.getX(), pt.getY()));
         dLength = 0;
         iOrder = m_Network.getCellValueAsInt(x, y);
         bContinue = true;
         do {
            iDirection = m_DEM.getDirToNextDownslopeCell(x, y);
            if (iDirection >= 0) {
               ix = x + m_iOffsetX[iDirection];
               iy = y + m_iOffsetY[iDirection];
               cell = new GridCell(ix, iy, m_DEM.getCellValueAsDouble(ix, iy));
               pt = extent.getWorldCoordsFromGridCoords(cell);
               coordsList.add(new Coordinate(pt.getX(), pt.getY()));
               dLength += m_DEM.getDistToNeighborInDir(iDirection);
               iIndexDownslope = m_HeadersAndJunctions.indexOf(cell);
               if (iIndexDownslope != -1) {
                  bContinue = false;
               }
               x = ix;
               y = iy;
            }
            else {
               bContinue = false;
            }

         }
         while (bContinue && !m_Task.isCanceled());

         values[0] = new Integer(i);
         values[1] = new Double(dLength);
         values[2] = new Integer(iOrder);
         values[3] = new Integer(iIndexDownslope);

         final Coordinate coords[] = new Coordinate[coordsList.size()];
         for (int j = 0; j < coords.length; j++) {
            coords[j] = (Coordinate) coordsList.get(j);
         }
         final Geometry geom = new GeometryFactory().createLineString(coords);
         network.addFeature(geom, values);
      }
   }
}
