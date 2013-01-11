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
}
