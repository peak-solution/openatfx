// Start MDM community Header =======================================
// 
// This software has been developed an published under the conditions 
// of the MDM community software exchange. See terms of use in the
// attached file
// 
// audi-mdm-community-eula-110323-1.pdf 
//      MD5 Hash 62FCB71DBDBFBF624A1DECC83B8835B3 
// MDMCommunityGTaC_v3_20110503.pdf 
//      MD5 Hash E9639F03804DAE3333F4557BE3F41D1C 
// 
// Any further distribution or use has to be realized under these 
// conditions. You are not allowed to change or modify this header.  
// 
// For further information see MDM community web site 
// 
// https://www.openmdm.org 
//
// Following to this header, secondary information shall be included: 
// 
//      Publisher: science+computing ag
//      Author: Dr. Olaf Flebbe
//      Publishing date: 7.5.2013
//      Version: 1.0.0
// 
// Stop  MDM community Header =======================================

package de.rechner.openatfx.util;

/**
 * Bit with Number Interface
 * 
 * @author Olaf Flebbe o.flebbe@science-computing.de
 */
public class Bit extends Number {

    private static final long serialVersionUID = -4780315505841860471L;
    private boolean _bit;

    public Bit(int value) {
        switch (value) {
            case 1:
                _bit = true;
            break;
            case 0:
                _bit = false;
            break;
            default:
                throw new RuntimeException("Bit not 1 or 0");
        }
    }

    public Bit(boolean value) {
        _bit = value;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return (_bit ? 1 : 0);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return (_bit ? 1 : 0);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        return (_bit ? 1.f : 0.f);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        return (_bit ? 1. : 0.);
    }

}
