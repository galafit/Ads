package comport;


import com.crostec.ads.AdsConfiguration;
import com.crostec.gui.comport_gui.ComportModel;

/**
 * Created by gala on 29/10/16.
 */
public class ComportFacade implements ComportModel {
    @Override
    public String[] getAvailableComports() {
        return ComPort.getportNames();
    }
}