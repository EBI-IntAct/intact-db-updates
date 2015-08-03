package uk.ac.ebi.intact.dbupdate.gene.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
public class ParameterNameValue
{
    private final String name;
    private final String value;

    public ParameterNameValue(String name, String value)
            throws UnsupportedEncodingException
    {
        this.name = URLEncoder.encode(name, "UTF-8");
        this.value = URLEncoder.encode(value, "UTF-8");
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
