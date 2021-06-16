package com.oracle.coherence.examples;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

@Portable
public class MyEntryProcessor
        extends AbstractProcessor<Integer, Person, Void> {

    @PortableProperty(0)
    private String address;

    public MyEntryProcessor() {
    }
    
    public MyEntryProcessor(String address) {
        this.address = address;
    }

    @Override
    public Void process(InvocableMap.Entry<Integer, Person> entry) {
        Person c = entry.getValue();
        c.setAddress(address);
        entry.setValue(c);
        return null;
    }
}
