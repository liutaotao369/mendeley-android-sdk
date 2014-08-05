package com.mendeley.api.params;


public class GroupRequestParameters extends MendeleyRequest {

    /**
     * The maximum number of items on the page. If not supplied, the default is 20. The largest allowable value is 500.
     */
    public Integer limit;

    /**
     * A marker for the last key in the previous page
     */
    public String marker;

}
