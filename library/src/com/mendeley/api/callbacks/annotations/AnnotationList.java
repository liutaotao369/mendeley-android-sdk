package com.mendeley.api.callbacks.annotations;

import com.mendeley.api.model.Annotation;
import com.mendeley.api.model.Document;
import com.mendeley.api.params.Page;

import java.util.Date;
import java.util.List;

public class AnnotationList {
    public final List<Annotation> annotations;
    public final Page next;
    public final Date serverDate;

    public AnnotationList(List<Annotation> annotations, Page next, Date serverDate) {
        this.annotations = annotations;
        this.next = next;
        this.serverDate = serverDate;
    }
}
