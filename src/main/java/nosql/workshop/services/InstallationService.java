package nosql.workshop.services;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import net.codestory.http.Context;
import nosql.workshop.model.Installation;
import nosql.workshop.model.stats.Average;
import nosql.workshop.model.stats.CountByActivity;
import nosql.workshop.model.stats.InstallationsStats;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


/**
 * Service permettant de manipuler les installations sportives.
 */
@Singleton
public class InstallationService {
    /**
     * Nom de la collection MongoDB.
     */
    public static final String COLLECTION_NAME = "installations";

    private final MongoCollection installations;

    @Inject
    public InstallationService(MongoDB mongoDB) throws UnknownHostException {
        this.installations = mongoDB.getJongo().getCollection(COLLECTION_NAME);
    }

    public Installation random() {
        MongoClient mongoClient = new MongoClient();
        DB db = mongoClient.getDB("nosql-workshop");
        Jongo jongo = new Jongo(db);
        MongoCollection installations = jongo.getCollection("installations");
        long size = installations.count();
        Installation installation = installations.find().limit(1).skip((int) Math.floor(Math.random() * size)).as(Installation.class).next();
        return installation;
    }

    public List<Installation> getInstallations(){
        MongoClient mongoClient = new MongoClient();
        DB db = mongoClient.getDB("nosql-workshop");
        Jongo jongo = new Jongo(db);
        MongoCollection installations = jongo.getCollection("installations");
        MongoCursor<Installation> all = installations.find().as(Installation.class);
        List<Installation> result = new ArrayList<>();
        for(Installation installation : all){
            result.add(installation);
        }
        return result;
    }

    public Installation getId(String id){
        DB db = new MongoClient().getDB("nosql-workshop");
        Jongo jongo = new Jongo(db);
        String query = "{_id : '"+id+"'}";
        return jongo.getCollection("installations").findOne(query).as(Installation.class);
    }

    public List<Installation> geoSearch(Context search){
        String lat = search.get("lat");
        String lng = search.get("lng");
        String distance = search.get("distance");
        DB db = new MongoClient().getDB("nosql-workshop");
        Jongo jongo = new Jongo(db);
        String query = "{location:{$near:{$geometry:{type:'Point', coordinates: ["+lng+", "+lat+"]}, $maxDistance: "+distance+"}}}";
        MongoCollection installations = jongo.getCollection("installations");
        installations.ensureIndex("{ \"location\" : \"2dsphere\" }");
        MongoCursor<Installation> all = installations.find(query).as(Installation.class);
        List<Installation> result = new ArrayList<>();
        for(Installation installation : all){
            result.add(installation);
        }
        return result;
    }

    public InstallationsStats getStats() {
        InstallationsStats installationsStats = new InstallationsStats();
        Average averagePerInstallation;
        ArrayList<CountByActivity> countByActivity;
        Installation maxEquipements;
        installationsStats.setTotalCount(installations.count());

        averagePerInstallation = installations
                                    .aggregate("{$group: {_id: null, average: {$avg : {$size: \"$equipements\"}}}}")
                                    .as(Average.class)
                                    .next();
        installationsStats.setAverageEquipmentsPerInstallation(averagePerInstallation.getAverage());

        countByActivity = Lists.newArrayList(installations
                                                .aggregate("{$unwind: \"$equipements\"}")
                                                .and("{$unwind: \"$equipements.activites\"}")
                                                .and("{$group: {_id: \"$equipements.activites\", total:{$sum : 1}}}")
                                                .and("{$sort : { total : -1 }}")
                                                .and("{$project: {activite: \"$_id\", total : 1}}")
                                                .as(CountByActivity.class).iterator());
        installationsStats.setCountByActivity(countByActivity);

        maxEquipements = installations
                            .aggregate("{$project : {_id : 1, nom: 1, equipements : 1, size : {$size: \"$equipements\"}}}")
                            .and("{$sort : {size : -1}}}")
                            .and("{$limit : 1}")
                            .as(Installation.class).next();
        installationsStats.setInstallationWithMaxEquipments(maxEquipements);

        return installationsStats;
    }

}
