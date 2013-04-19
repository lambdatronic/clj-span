

package es.unex.sextante.hydrology.setLinesDirectionsWithDEM;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.dataObjects.vectorFilters.BoundingBoxFilter;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.outputs.OutputVectorLayer;


public class SetLinesDirectionsWithDEMAlgorithm
         extends
            GeoAlgorithm {

   public static final String RESULT = "RESULT";
   public static final String LINES  = "LINES";
   public static final String DEM    = "DEM";

   private IVectorLayer       m_Output;
   private ArrayList          m_Lines;
   private IRasterLayer       m_DEM;


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Change_line_direction"));
      setGroup(Sextante.getText("Tools_for_line_layers"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputVectorLayer(LINES, Sextante.getText("Lines"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
         addOutputVectorLayer(RESULT, Sextante.getText("Lines"), OutputVectorLayer.SHAPE_TYPE_LINE, LINES);
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);

      final IVectorLayer lines = m_Parameters.getParameterValueAsVectorLayer(LINES);
      if (!m_bIsAutoExtent) {
         lines.addFilter(new BoundingBoxFilter(m_AnalysisExtent));
      }

      m_Output = getNewVectorLayer(RESULT, Sextante.getText("Result"), IVectorLayer.SHAPE_TYPE_LINE, lines.getFieldTypes(),
               lines.getFieldNames());

      int i = 0;
      final int iShapeCount = lines.getShapesCount();
      final IFeatureIterator iter = lines.iterator();
      final GeometryFactory gf = new GeometryFactory();
      while (iter.hasNext() && setProgress(i, iShapeCount)) {
         final IFeature feature = iter.next();
         final Geometry geom = feature.getGeometry();
         m_Lines = new ArrayList();
         for (int j = 0; j < geom.getNumGeometries(); j++) {
            final Geometry line = geom.getGeometryN(j);
            addLine(line);
         }
         final LineString[] lineStrings = new LineString[m_Lines.size()];
         for (int j = 0; j < lineStrings.length; j++) {
            lineStrings[j] = (LineString) m_Lines.get(j);
         }
         m_Output.addFeature(gf.createMultiLineString(lineStrings), feature.getRecord().getValues());
         i++;
      }
      iter.close();

      return !m_Task.isCanceled();

   }


   private void addLine(final Geometry line) {


      final Coordinate[] coords = line.getCoordinates();
      final double z = m_DEM.getValueAt(coords[0].x, coords[0].y);
      final double z2 = m_DEM.getValueAt(coords[coords.length - 1].x, coords[coords.length - 1].y);

      if (z2 > z) {
         final Coordinate[] newCoords = new Coordinate[coords.length];
         for (int i = 0; i < coords.length; i++) {
            newCoords[i] = coords[coords.length - i - 1];
         }
         final GeometryFactory gf = new GeometryFactory();
         m_Lines.add(gf.createLineString(newCoords));
      }
      else {
         m_Lines.add(line);
      }

   }


}
