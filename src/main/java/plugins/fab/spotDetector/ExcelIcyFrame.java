package plugins.fab.spotDetector;

import icy.gui.frame.IcyFrame;
import icy.system.thread.ThreadUtil;
import jxl.write.WritableSheet;

public class ExcelIcyFrame
{

    /**
     * Display an XLS page
     * Work in progress
     * should be a XlsManager with a multi page support...
     * copy / paste and so on...
     * 
     * @param name
     *        name of the frame
     * @param writableSheet
     *        excel sheet to display
     * @author Fabrice de Chaumont
     */
    public ExcelIcyFrame(final String name, final WritableSheet writableSheet)
    {

        ThreadUtil.invokeLater(new Runnable()
        {

            @Override
            public void run()
            {
                IcyFrame frame = new IcyFrame(name);
                frame.getContentPane().add(new ExcelGrid(writableSheet));
                frame.pack();
                frame.setVisible(true);
            }

        });

    }

}
