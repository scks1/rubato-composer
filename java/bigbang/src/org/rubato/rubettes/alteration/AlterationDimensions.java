package org.rubato.rubettes.alteration;

/**
 * The dimensions an {@link Alterator} alters two compositions along: the paths
 * of the altered values and the degree of alteration at the start and end of
 * each dimension. The Alteration rubette's dimensions table implements it.
 */
public interface AlterationDimensions {
	
	public int[][] getPaths();
	
	public int[][] getElementPaths();
	
	public double[] getStartDegrees();
	
	public double[] getEndDegrees();
	
	public int[][] getRelativeToFormPaths();
	
	public int[][] getDifferentRelativeToFormPaths();

}
