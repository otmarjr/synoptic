package synoptic.statistics;

import java.util.LinkedHashMap;
import java.util.Set;

import synoptic.model.IEvent;

public class FrequencyMiner<T extends IEvent> {
    LinkedHashMap<String, Integer> frequencies = new LinkedHashMap<String, Integer>();

    public FrequencyMiner(Set<T> events) {
        for (T e : events) {
            if (!frequencies.containsKey(e.getName())) {
                frequencies.put(e.getName(), new Integer(0));
            }
            frequencies.put(e.getName(), frequencies.get(e.getName()) + 1);
        }
    }

    public LinkedHashMap<String, Integer> getFrequencies() {
        return frequencies;
    }

    @Override
    public String toString() {
        return frequencies.toString();
    }
}
