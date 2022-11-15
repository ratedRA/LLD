package com.ratedra.pedencysystemaman;

import java.util.*;
import java.util.stream.Collectors;

enum TrackingStatus{
    STARTED,
    STOPPED;
}
class Tracker{
    private String id;

    private String entityPrefix;
    private TrackingStatus status;

    private String hashedTags;

    public Tracker(String id, String entityPrefix, TrackingStatus status) {
        this.id = id;
        this.entityPrefix = entityPrefix;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TrackingStatus getStatus() {
        return status;
    }

    public void setStatus(TrackingStatus status) {
        this.status = status;
    }

    public String getHashedTags() {
        return hashedTags;
    }

    public void setHashedTags(String hashedTags) {
        this.hashedTags = hashedTags;
    }

    public String getEntityPrefix() {
        return entityPrefix;
    }

    public void setEntityPrefix(String entityPrefix) {
        this.entityPrefix = entityPrefix;
    }
}

class TrackingDao{
    Map<String, List<Tracker>> trackersByEntity;
    private TrackingDao(){
        trackersByEntity = new HashMap<>();
    }

    private static TrackingDao INSTANCE = null;
    public static TrackingDao getInstance(){
        if(INSTANCE == null){
            INSTANCE = new TrackingDao();
        }
        return INSTANCE;
    }

    public void createOrUpdate(Tracker tracker){
        if(trackersByEntity.containsKey(tracker.getEntityPrefix())) {
            if(dedupe(tracker)){
                return;
            }
            trackersByEntity.get(tracker.getEntityPrefix()).add(tracker);
        } else {
            List<Tracker> trackers = new ArrayList<>();
            trackers.add(tracker);
            trackersByEntity.put(tracker.getEntityPrefix(), trackers);
        }
    }

    private boolean dedupe(Tracker tracker) {
        boolean dedupeReturn = false;
        List<Tracker> trackersForEntity = trackersByEntity.get(tracker.getEntityPrefix());
        Optional<Tracker> dedupe = trackersForEntity.stream().filter(t -> t.getId() == tracker.getId() && t.getEntityPrefix() == tracker.getEntityPrefix() && t.getStatus() == TrackingStatus.STARTED).findAny();
        if(tracker.getStatus() == TrackingStatus.STARTED && dedupe.isPresent()){
            System.out.println("tracker for this id already exists");
            dedupeReturn = true;
        }
        return dedupeReturn;
    }

    public void stopTracker(String entityPrefix, String id){
        List<Tracker> trackers = trackersByEntity.get(entityPrefix);
        for(Tracker tracker : trackers){
            if(tracker.getId().equals(id)){
                tracker.setStatus(TrackingStatus.STOPPED);
            }
        }
    }

    public List<Tracker> getTracker(String entityPrefix){
        return trackersByEntity.get(entityPrefix);
    }

}

interface OrderedTag{
    int getOrder();

    String getTagPrefix();
}

interface TrackableEntity{
    String getEntityPrefix();
    List<String> getTagPrefixes();
}

class FlightEntity implements TrackableEntity{
    enum FlightTag implements OrderedTag{
        INSTRUMENT("instrument-", 1),
        STATE("state-", 2),
        CITY("city-", 3);

        private int order;
        private String tagPrefix;

        FlightTag(String tagPrefix, int order) {
            this.order = order;
            this.tagPrefix = tagPrefix;
        }


        @Override
        public int getOrder() {
            return this.order;
        }

        @Override
        public String getTagPrefix() {
            return this.tagPrefix;
        }
    }

    @Override
    public String getEntityPrefix() {
        return "FLIGHT-";
    }

    @Override
    public List<String> getTagPrefixes() {
        FlightTag[] values = FlightTag.values();
        return Arrays.asList(values).stream().map(value -> value.tagPrefix).collect(Collectors.toList());
    }
}

interface TrackingService{

    TrackableEntity getEntity();
    void startTracking(String id, List<String> tags);
    void stopTracking(String id);
    Integer getCounts(List<String> tags);
}

class FlightTrackingService implements TrackingService{

    private TrackingDao trackingDao;

    public FlightTrackingService() {
        this.trackingDao = TrackingDao.getInstance();
    }

    @Override
    public TrackableEntity getEntity() {
        return new FlightEntity();
    }

    @Override
    public void startTracking(String id, List<String> tags) {
        TrackableEntity entity = getEntity();
        String entityPrefix = entity.getEntityPrefix();
        List<String> tagPrefixes = entity.getTagPrefixes();
        if(tagPrefixes.size() != tags.size()){
            throw new IllegalArgumentException();
        }
        String hashedTags = buildInputSearchHash(tags, tagPrefixes.size(), tagPrefixes);
        Tracker tracker = new Tracker(id, entityPrefix, TrackingStatus.STARTED);
        tracker.setHashedTags(hashedTags);
        trackingDao.createOrUpdate(tracker);
    }

    @Override
    public void stopTracking(String id) {
        trackingDao.stopTracker(getEntity().getEntityPrefix(), id);
    }

    @Override
    public Integer getCounts(List<String> tags) {
        List<String> ids = new ArrayList<>();
        List<Tracker> trackers = trackingDao.getTracker(getEntity().getEntityPrefix());
        List<Tracker> activeTrackers = trackers.stream().filter(tracker -> TrackingStatus.STARTED == tracker.getStatus()).collect(Collectors.toList());
        int searchTagSize = tags.size();

        List<String> tagPrefixes = getEntity().getTagPrefixes();
        String inputSearchHash = buildInputSearchHash(tags, searchTagSize, tagPrefixes);

        for(Tracker tracker : activeTrackers){
            String hashedTags = tracker.getHashedTags();
            String[] split = hashedTags.split("#");
            String searchText = buildSearchText(tags, split);
            if(searchText.equals(inputSearchHash)){
                ids.add(tracker.getId());
            }
        }
        return ids.size();
    }

    private static String buildInputSearchHash(List<String> tags, int searchTagSize, List<String> tagPrefixes) {
        int itr = 0;
        StringBuilder stringBuilder = new StringBuilder("");
        while (itr < searchTagSize){
            if(itr !=0){
                stringBuilder.append("#");
            }
            stringBuilder.append(tagPrefixes.get(itr)+ tags.get(itr));
            itr++;
        }
        String inputSearchHash = stringBuilder.toString();
        return inputSearchHash;
    }

    private static String buildSearchText(List<String> tags, String[] split) {
        String searchText = "";
        for(int ind = 0; ind < tags.size(); ind++){
            if(ind != 0){
                searchText += "#";
            }
            searchText += split[ind];
        }
        return searchText;
    }
}

public class PendencySystemLLD {
    public static void main(String[] args) {
        FlightTrackingService flightTrackingService = new FlightTrackingService();
        List<String> tags1 = new ArrayList<>();
        tags1.add("UPI");
        tags1.add("Karnataka");
        tags1.add("Bangalore");
        flightTrackingService.startTracking("1234", tags1);
        List<String> tags2 = new ArrayList<>();
        tags2.add("wallet");
        tags2.add("Karnataka");
        tags2.add("Mysore");
        flightTrackingService.startTracking("2365", tags2);
        List<String> tags4 = new ArrayList<>();
        tags4.add("UPI");
        tags4.add("Karnataka");
        tags4.add("Mysore");
        flightTrackingService.startTracking("2364", tags4);
        List<String> tags7 = new ArrayList<>();
        tags7.add("UPI");
        tags7.add("Karnataka");
        tags7.add("Mysore");
        flightTrackingService.startTracking("2364", tags7);

        flightTrackingService.stopTracking("2364");
        List<String> tags3 = new ArrayList<>();
        tags3.add("wallet");
        //tags3.add("Karnataka");
        //tags3.add("Mysore");
        Integer counts = flightTrackingService.getCounts(tags3);
        System.out.println(counts);
        //flightTrackingService.stopTracking();
        List<String> tags8 = new ArrayList<>();
        tags8.add("UPI");
        tags8.add("Karnataka");
        Integer counts2 = flightTrackingService.getCounts(tags8);
        System.out.println(counts2);
    }
}
