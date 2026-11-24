package org.rubato.rubettes.bigbang.model.operations;

import java.util.Arrays;
import java.util.logging.Logger;

import org.rubato.math.matrix.RMatrix;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

public class AffineTransformation extends AbstractLocalTransformation {
	
	private double[] shift;
	private RMatrix transform;
	
	//used for cloning
	protected AffineTransformation(BigBangModel model, AffineTransformation other) {
		super(model, other);
		this.setParameters(other.shift, other.transform);
	}
	
	public AffineTransformation(BigBangModel model, TransformationProperties properties, double[] shift, RMatrix transform2x2) {
		super(model, properties);
		//System.out.println(properties.getCenter()[0] + " " + properties.getCenter()[1]);
		this.setParameters(shift, transform2x2);
	}
	
	public AffineTransformation(BigBangModel model, XMLReader reader, Element element) {
		super(model, reader, element);
		this.fromXML(element);
	}
	
	public void setParameters(double[] shift, RMatrix transform2x2) {
		this.shift = shift;
		this.transform = transform2x2;
		this.updateOperation();
	}
	
	/**
	 * Creates a copy whose full map is this operation's map at the given ratio, as the other
	 * local transformations' copies are: the matrix r A + (1 - r) I and the shift r b.
	 */
	protected AffineTransformation createModifiedCopy(double ratio) {
		AffineTransformation modifiedCopy = (AffineTransformation)this.clone();
		modifiedCopy.setParameters(AffineTransformation.interpolatedShift(this.shift, ratio),
				AffineTransformation.interpolatedMatrix(this.transform, ratio));
		return modifiedCopy;
	}
	
	/**
	 * The matrix at the current modification ratio r, r A + (1 - r) I, interpolated from the
	 * identity as Mazzola, The Topos of Music III, section 73.2.1.6, p. 1154 gesturalizes
	 * affine transformations. At r = 1 it is A.
	 */
	@Override
	protected RMatrix getMatrix() {
		return AffineTransformation.interpolatedMatrix(this.transform, this.modificationRatio);
	}
	
	/** The shift at the current modification ratio r, r b (section 73.2.1.6). */
	protected double[] getShift() {
		return AffineTransformation.interpolatedShift(this.shift, this.modificationRatio);
	}
	
	private static RMatrix interpolatedMatrix(RMatrix matrix, double ratio) {
		if (ratio == 1) {
			return matrix;
		}
		return new RMatrix(new double[][]{
			{ratio*matrix.get(0, 0) + (1-ratio), ratio*matrix.get(0, 1)},
			{ratio*matrix.get(1, 0), ratio*matrix.get(1, 1) + (1-ratio)}});
	}
	
	private static double[] interpolatedShift(double[] shift, double ratio) {
		if (ratio == 1) {
			return shift;
		}
		return new double[]{ratio*shift[0], ratio*shift[1]};
	}
	
	@Override
	protected String getSpecificPresentationName() {
		return "Affine Transformation";
	}

	private static final String AFFINE_TAG = "Affine";
	private static final String SHIFT_ATTR = "shift";
	private static final String ROWS_ATTR = "rows";
	private static final String MATRIX_ATTR = "matrix";
	
	public void toXML(XMLWriter writer) {
		super.toXML(writer);
		int rows = this.transform.getRowCount();
		int cols = this.transform.getColumnCount();
		double[] entries = new double[rows*cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				entries[i*cols+j] = this.transform.get(i, j);
			}
		}
		writer.empty(AFFINE_TAG, SHIFT_ATTR, Arrays.toString(this.shift),
				ROWS_ATTR, rows, MATRIX_ATTR, Arrays.toString(entries));
	}
	
	/**
	 * Reads the shift and matrix written by toXML. Files saved before the Affine element
	 * existed hold no parameters for this operation; it then loads as the identity map
	 * and a warning names the loss.
	 */
	private void fromXML(Element element) {
		Element affineElement = XMLReader.getChild(element, AFFINE_TAG);
		if (affineElement == null) {
			Logger.getLogger(AffineTransformation.class.getName()).warning(
					"Affine transformation saved without its parameters; loading it as the identity map");
			this.setParameters(new double[2], RMatrix.getUnitMatrix(2));
			return;
		}
		double[] shift = XMLReader.getDoubleArrayAttribute(affineElement, SHIFT_ATTR);
		int rows = XMLReader.getIntAttribute(affineElement, ROWS_ATTR, 2);
		double[] entries = XMLReader.getDoubleArrayAttribute(affineElement, MATRIX_ATTR);
		int cols = entries.length/rows;
		double[][] values = new double[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				values[i][j] = entries[i*cols+j];
			}
		}
		this.setParameters(shift, new RMatrix(values));
	}

}
