package de.csbd.learnathon.command;


import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class FlowComputation {

	public static Img< FloatType > getTMFlow( Img< FloatType > img ) {
		float sigma = 2;
		Img< FloatType > smoothed_img = gaussian_smoothing2D( img, sigma );
		Object localMaxima = findLocalMax( img, 5 );
		
		
		return flow;

	}

	private static ArrayList<LocalMaximaTriplet> findLocalMax( Img< FloatType > img, int r ) {
		 Cursor<FloatType> cursor= Views.iterable(img).cursor();
		 ArrayList<LocalMaximaTriplet> localMaxList = new ArrayList<>();
		 while ( cursor.hasNext() )
	        {
	            cursor.fwd();
	            HyperSphere< FloatType > smallSphere = new HyperSphere<>( img, cursor, r );
	            FloatType centerValue=cursor.get();
	            boolean isMaximum = true;
	            
	            for ( final FloatType value : smallSphere ) {
	            	if ( centerValue.compareTo( value ) < 0 ) {
						isMaximum = false;
						break;
					}
	            }
	            if ( isMaximum ) {
	            	
	            	localMaxList.add(new LocalMaximaTriplet( cursor.getIntPosition(0), cursor.getIntPosition(0), centerValue.get()));
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
