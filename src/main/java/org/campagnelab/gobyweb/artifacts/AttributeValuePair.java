package org.campagnelab.gobyweb.artifacts;

/**
 * @author Fabien Campagne
 *         Date: 1/4/13
 *         Time: 5:19 PM
 */
public class AttributeValuePair {
    String name;
    String value;

    public AttributeValuePair(String attribute, String value) {
        this.name = attribute;
        this.value = value;
    }

    public AttributeValuePair(String attribute) {
        this.name=attribute;
    }

    @Override
    public int hashCode() {
        if (value==null) {return name.hashCode();}
        return name.hashCode() ^ value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttributeValuePair)) {
            return false;
        }
        AttributeValuePair other= (AttributeValuePair) o;
        return name.equals(other.name) && value.equals(other.value);
    }

    @Override
    public String toString() {
        return String.format("%s=%s",name, value!=null?value : "<undefined>");
    }
}
