package de.rechner.openatfx.exporter;

class ExportRelConfig {

    private final String elem1BeName;
    private final String elem2BeName;

    public ExportRelConfig(String elem1BeName, String elem2BeName) {
        this.elem1BeName = elem1BeName.toLowerCase();
        this.elem2BeName = elem2BeName.toLowerCase();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elem1BeName == null) ? 0 : elem1BeName.hashCode());
        result = prime * result + ((elem2BeName == null) ? 0 : elem2BeName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExportRelConfig other = (ExportRelConfig) obj;
        if (elem1BeName == null) {
            if (other.elem1BeName != null)
                return false;
        } else if (!elem1BeName.equals(other.elem1BeName))
            return false;
        if (elem2BeName == null) {
            if (other.elem2BeName != null)
                return false;
        } else if (!elem2BeName.equals(other.elem2BeName))
            return false;
        return true;
    }

}
