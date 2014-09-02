package supplychain.simulator;

import java.util.List;

/**
 * Created by rene on 02.09.14.
 */
public class NcdcDailySummaryResult {

    private List<NcdcDailySummary> results;
    private NcdcMetadata metadata;

    public List<NcdcDailySummary> getResults() {
        return results;
    }

    public void setResults(List<NcdcDailySummary> results) {
        this.results = results;
    }

    public NcdcMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(NcdcMetadata metadata) {
        this.metadata = metadata;
    }

    class NcdcDailySummary {
        private String station;
        private int value;
        private String attributes;
        private String datatype;
        private String date;

        public String getStation() {
            return station;
        }

        public void setStation(String station) {
            this.station = station;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getAttributes() {
            return attributes;
        }

        public void setAttributes(String attributes) {
            this.attributes = attributes;
        }

        public String getDatatype() {
            return datatype;
        }

        public void setDatatype(String datatype) {
            this.datatype = datatype;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    class NcdcResultSet {
        private int count;
        private int limit;
        private int offset;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }

    class NcdcMetadata {
        private NcdcResultSet resultset;

        public NcdcResultSet getResultset() {
            return resultset;
        }

        public void setResultset(NcdcResultSet resultset) {
            this.resultset = resultset;
        }
    }


}
