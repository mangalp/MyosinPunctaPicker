package de.csbd.learnathon.command;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TiffLoader {

	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > loadFlowFieldFromDirectory() {
		final JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );
		chooser.setDialogTitle( "Choose a directory to load flow fields from: " );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		chooser.showSaveDialog( null );

		File file = chooser.getSelectedFile();
		final RandomAccessibleInterval< T > img =
				ImagePlusAdapter.wrapReal( IJ.openImage( file.getAbsolutePath() ) );
		return img;
	}

}