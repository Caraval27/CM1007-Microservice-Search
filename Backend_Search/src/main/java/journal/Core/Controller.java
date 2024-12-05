package journal.Core;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import journal.Core.Model.PatientData;
import org.hl7.fhir.r4.model.Patient;

import java.util.ArrayList;
import java.util.List;

@Path("/")
public class Controller {

    @Inject
    HapiService hapiService;

    @GET
    @Path("patients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatients() {
        try {
            List<Patient> patients = hapiService.getPatientsByIdentifierSystem();
            List<PatientData> patientsData = new ArrayList<>();
            for (Patient patient : patients) {
                patientsData.add(hapiService.getPatientData(patient));
            }
            return Response.ok(patientsData).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}