package nosql.workshop.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import nosql.workshop.model.Equipement;
import nosql.workshop.model.Installation;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nosql.workshop.model.Installation.*;

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
        // FIXME : bien sûr ce code n'est pas le bon ... peut être quelque chose comme installations.findOne()
        Installation installation = new Installation();
        installation.setNom("Mon Installation");
        installation.setEquipements(Arrays.asList(new Equipement()));
        installation.setAdresse(new Adresse());
        Location location = new Location();
        location.setCoordinates(new double[]{3.4, 3.2});
        installation.setLocation(location);
        return installation;
    }

    public List<Installation> getInstallations(){
        MongoClient mongoClient = new MongoClient();
        DB db = mongoClient.getDB("nosql-workshop");
        //MongoDatabase db = mongoClient.getDatabase("nosql-workshop");

        Jongo jongo = new Jongo(db);
        MongoCollection installations = jongo.getCollection("installations");
        MongoCursor<Installation> all = installations.find().as(Installation.class);
        List<Installation> result = new ArrayList<>();
        for(Installation installation : all){
            result.add(installation);
        }
        return result;
    }
}
