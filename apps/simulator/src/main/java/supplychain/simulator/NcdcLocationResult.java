package supplychain.simulator;

import java.util.List;

/**
 * Created by rene on 02.09.14.
 */
public class NcdcLocationResult {

    private List<NcdcLocation> results;
    private NcdcMetadata metadata;

    public NcdcMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(NcdcMetadata metadata) {
        this.metadata = metadata;
    }

    public List<NcdcLocation> getResults() {
        return results;
    }

    public void setResults(List<NcdcLocation> results) {
        this.results = results;
    }

    class NcdcLocation {
        private String id;
        private Double longitude;
        private Double latitude;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    class NcdcResultSet {
        private int count;
        private int limit;
        private int offset;

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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
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
