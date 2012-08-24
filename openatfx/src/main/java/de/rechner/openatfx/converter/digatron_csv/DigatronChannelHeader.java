package de.rechner.openatfx.converter.digatron_csv;

import org.asam.ods.DataType;

import de.rechner.openatfx.util.ODSHelper;


class DigatronChannelHeader {

    private String channelName;
    private String unitName;
    private DataType dataType;

    public DigatronChannelHeader(String channelName, String unitName, DataType dataType) {
        this.channelName = channelName;
        this.unitName = unitName;
        this.dataType = dataType;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return "DigatronChannelHeader [channelName=" + channelName + ", unitName=" + unitName + ", dataType="
                + ODSHelper.dataType2String(dataType) + "]";
    }

}
