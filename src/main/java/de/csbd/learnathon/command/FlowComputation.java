package de.csbd.learnathon.command;


import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class FlowComputation {

	public static Img< FloatType > getTMFlow( Img< FloatType > img ) {
		float sigma = 2;
		Img< FloatType > smoothed_img = gaussian_smoothing2D( img, sigma );
		ArrayList< LocalMaximaQuartet > localMaxima = findLocalMax( img, 5 );
		ArrayList< LocalMaximaQuartet > thresholdedLocalMaxima = thresholdedMaxima( localMaxima, 20 );
		ArrayList< FlowVector > sparseFlow = templateMatching( smoothed_img, thresholdedLocalMaxima );
		Img< FloatType > denseFlow = interpolateFlowNN( sparseFlow,smoothed_img );
		return denseFlow;
	}

	private static Img< FloatType > interpolateFlowNN( ArrayList< FlowVector > sparseFlow,Img< FloatType > img ) {

//		final ImgFactory< FloatType > imgFactory = new CellImgFactory<>( new FloatType(), 5 );
//		final Img< FloatType > denseFlow  = imgFactory.create( img.dimension( 0 ), img.dimension( 1 ),img.dimension( 2 )*2-2  );
		
//		final ImgFactory< FloatType > imgFactory = new CellImgFactory<>( new FloatType(), 5 );
//		final Img< FloatType > denseFlow  = imgFactory.create( img.dimension( 0 ), img.dimension( 1 ),0  );		
		
		ArrayList<RandomAccessibleInterval< FloatType >> slices=new ArrayList<RandomAccessibleInterval< FloatType >>();
		
		for ( long pos = 0; pos < img.dimension( 2 )-1; ++pos ) {
			 RandomAccessibleInterval< FloatType > slice = Views.hyperSlice( img, 2, pos );
			
			 ///FinalInterval interval = new FinalInterval( new long[] { 375, 200 } );
			
			 RealPointSampleList< FloatType > realIntervalU = new RealPointSampleList<>( 2 );
			 RealPointSampleList< FloatType > realIntervalV = new RealPointSampleList<>( 2 );
			 for ( FlowVector f : sparseFlow ) { 
				 if (f.getT()==pos){ 
					 RealPoint point = new RealPoint( 2 );
					 point.setPosition(f.getX(),0);
					 point.setPosition(f.getY(),1);
					 
					 realIntervalU.add(point, new FloatType(f.getU()));
					 realIntervalV.add(point, new FloatType(f.getV()));
				 }
			 }
			 
			 
			 
			 NearestNeighborSearch< FloatType > searchU =new NearestNeighborSearchOnKDTree<>(new KDTree<>( realIntervalU ) );
			 NearestNeighborSearch< FloatType > searchV =new NearestNeighborSearchOnKDTree<>(new KDTree<>( realIntervalV ) );
			 
			 RealRandomAccessible< FloatType > realRandomAccessibleU =Views.interpolate( searchU, new NearestNeighborSearchInterpolatorFactory< FloatType >() );
			 RealRandomAccessible< FloatType > realRandomAccessibleV =Views.interpolate( searchV, new NearestNeighborSearchInterpolatorFactory< FloatType >() );
			 
			 RandomAccessible< FloatType > randomAccessibleU = Views.raster( realRandomAccessibleU );
			 RandomAccessible< FloatType > randomAccessibleV = Views.raster( realRandomAccessibleV );
			 
			 RandomAccessibleInterval< FloatType > sliceU = Views.interval( randomAccessibleU, slice );
			 RandomAccessibleInterval< FloatType > sliceV = Views.interval( randomAccessibleV, slice );
			 
			 slices.add(sliceU);
			 slices.add(sliceV);
			 
			
		}
		
		Img<FloatType> stack =(Img<FloatType>) Views.stack(slices);
	
		
		return stack;
	}

	private static ArrayList< LocalMaximaQuartet > findLocalMax( Img< FloatType > img, int r ) {

		ArrayList< LocalMaximaQuartet > localMaxList = new ArrayList<>();
		for ( long pos = 0; pos < img.dimension( 2 ); ++pos ) {
			RandomAccessibleInterval< FloatType > slice = Views.hyperSlice( img, 2, pos );
			Cursor< FloatType > cursor = Views.iterable( slice ).cursor();

			while ( cursor.hasNext() ) {
				cursor.fwd();
				HyperSphere< FloatType > smallSphere = new HyperSphere<>( slice, cursor, r );
				FloatType centerValue = cursor.get();
				boolean isMaximum = true;

				for ( final FloatType value : smallSphere ) {
					if ( centerValue.compareTo( value ) < 0 ) {
						isMaximum = false;
						break;
					}
				}
				if ( isMaximum ) {

					localMaxList
							.add( new LocalMaximaQuartet( cursor.getIntPosition( 0 ), cursor.getIntPosition( 0 ), ( int ) pos, centerValue.get() ) );
				}
			}

		}
		return localMaxList;
	}

	private static Img< FloatType > gaussian_smoothing2D( Img< FloatType > img, float sigma ) {

		float[] s = new float[ img.numDimensions() - 1 ];

		for ( int d = 0; d < s.length; ++d )
			s[ d ] = sigma;

		for ( long pos = 0; pos < img.dimension( 2 ); ++pos ) {
			RandomAccessibleInterval< FloatType > slice = Views.hyperSlice( img, 2, pos );
			Gauss3.gauss( sigma, Views.extendMirrorSingle( slice ), slice );
		}

		return img;
	}

	private static ArrayList< LocalMaximaQuartet > thresholdedMaxima( ArrayList< LocalMaximaQuartet > a, float threshold ) {
		int i = 0;
		while ( i < a.size() ) {
			if ( a.get( i ).getV() < threshold )
				a.remove( i );
			else
				i++;
		}

		return a;
	}

	private static ArrayList< FlowVector > templateMatching( Img< FloatType > img, ArrayList< LocalMaximaQuartet > quartet ) {
		RectangleShape rectangle = new RectangleShape( 5, false );
		ArrayList<FlowVector> flowVectorList = new ArrayList<>(); 
		int ii = 0;
		int jj = 0;

		for ( long pos = 0; pos < img.dimension( 2 )-1; ++pos ) {
			RandomAccessibleInterval< FloatType > slice_small = Views.hyperSlice( img, 2, pos );
			NeighborhoodsAccessible< FloatType > neighborhoods1 = rectangle.neighborhoodsRandomAccessible( slice_small );
			RandomAccess< Neighborhood< FloatType > > ran1 = neighborhoods1.randomAccess(); 
			RandomAccessibleInterval< FloatType > slice_big = Views.hyperSlice( img, 2, pos+1 );
			NeighborhoodsAccessible< FloatType > neighborhoods2 = rectangle.neighborhoodsRandomAccessible( slice_big );
			RandomAccess< Neighborhood< FloatType > > ran2 = neighborhoods2.randomAccess(); 
			for ( LocalMaximaQuartet q : quartet ) {
				if(q.getT() == pos) {
					
				
    				ran1.setPosition( new int [] {q.getX(), q.getY()} );
    				Neighborhood< FloatType > n1 = ran1.get();

    				double maxVal = -Double.MAX_VALUE;
    				for (int i = -5; i< 5; i++ ) {
    					for (int j = -5; j < 5; j++) {
    						ran2.setPosition( new int [] {q.getX()+i, q.getY()+j} );
    						Neighborhood< FloatType > n2 = ran2.get();
    						
    						Cursor<FloatType> cursorA = n1.cursor();
    				        Cursor<FloatType> cursorB = n2.cursor();
    				        double sum = 0;
    				        while (cursorA.hasNext()) {
    				            double valueA = cursorA.next().getRealDouble();
    				            double valueB = cursorB.next().getRealDouble();
    				            sum+=(valueA*valueB);
    //				            sum += Math.pow( valueA-valueB,2 );
    			
    				        }
    				        if(sum > maxVal) {
    				        	maxVal = sum;
    				        	ii = i;
    				        	jj = j;
    				        }
    						
    					}
    				}
    				
    				
    				
    				
    				
    				flowVectorList.add( new FlowVector( q.getX(), q.getY(), q.getT(), ii, jj ) );
			}
			}
				
				
		}	
			return flowVectorList;
	}

//	public static Img< FloatType > getConstantFlow( RandomAccessibleInterval< DoubleType > image ) {
//
//		final ImgFactory< FloatType > imgFactory = new CellImgFactory<>( new FloatType(), 5 );
//		final Img< FloatType > img1 = imgFactory.create( image.dimension( 0 ), image.dimension( 1 ), 8 * 2 - 2 );
//
//		Cursor< FloatType > cursorInput = img1.cursor();
//
//		while ( cursorInput.hasNext() ) {
//			cursorInput.fwd();
//			cursorInput.get().set( 5f );
//		}
//
//		return img1;
//	}
		
}
