package journal.Core;

import ca.uhn.fhir.context.FhirContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import journal.Core.Model.PatientData;
import journal.Core.Model.PractitionerData;
import org.hl7.fhir.r4.model.*;

import java.util.List;

@ApplicationScoped
public class HealthService {
    private FhirContext context;
    private IGenericClient client;

    private static final String HAPI_SERVER_URL = "https://hapi-fhir.app.cloud.cbh.kth.se/fhir";
    private static final String PATIENT_SYSTEM = "http://electronichealth.se/identifier/personnummer";
    private static final String PRACTITIONER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
    private static final String PRACTITIONER_ROLE_SYSTEM = "http://terminology.hl7.org/CodeSystem/practitioner-role";
    private static final String CONDITION_SYSTEM = "http://snomed.info/sct";

    public HealthService() {
        this.context = FhirContext.forR4();
        this.client = context.newRestfulGenericClient(HAPI_SERVER_URL);
    }

    public Multi<Patient> getPatientsByName(String name) {
        return Multi.createFrom().emitter(emitter -> {
            Bundle bundle = client.search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.hasSystemWithAnyCode(PATIENT_SYSTEM))
                    .where(Patient.NAME.contains().value(name))
                    .sort().ascending(Patient.NAME)
                    .returnBundle(Bundle.class)
                    .execute();

            processPatients(bundle, emitter);

            emitter.complete();
        });
    }

    public Multi<Patient> getPatientsByNameAndPractitionerIdentifier(String name, String identifierValue) {
        return Uni.createFrom().item(() -> getPractitionerByIdentifier(identifierValue))
            .onItem().transformToMulti(practitioner -> Multi.createFrom().emitter(emitter -> {
                Bundle bundle = client.search()
                        .forResource(Patient.class)
                        .where(Patient.IDENTIFIER.hasSystemWithAnyCode(PATIENT_SYSTEM))
                        .where(Patient.NAME.contains().value(name))
                        .where(Patient.GENERAL_PRACTITIONER.hasId("Practitioner/" + practitioner.getIdElement().getIdPart()))
                        .sort().ascending(Patient.NAME)
                        .returnBundle(Bundle.class)
                        .execute();

                processPatients(bundle, emitter);

                emitter.complete();
            }));
    }

    public Multi<Patient> getPatientsByConditionCode(String code) {
        return Multi.createFrom().emitter(emitter -> {
            Bundle bundle = client.search()
                    .forResource(Condition.class)
                    .where(Condition.CODE.exactly().systemAndCode(CONDITION_SYSTEM, code))
                    .include(Condition.INCLUDE_SUBJECT)
                    .returnBundle(Bundle.class)
                    .execute();

            processPatients(bundle, emitter);

            emitter.complete();
        });
    }

    public Multi<Patient> getPatientsByConditionCodeAndPractitionerIdentifier(String code, String identifierValue) {
        return Uni.createFrom().item(() -> getPractitionerByIdentifier(identifierValue))
            .onItem().transformToMulti(practitioner -> Multi.createFrom().emitter(emitter -> {
                Bundle bundle = client.search()
                        .forResource(Condition.class)
                        .where(Condition.CODE.exactly().systemAndCode(CONDITION_SYSTEM, code))
                        .where(Condition.SUBJECT.hasChainedProperty(Patient.GENERAL_PRACTITIONER.hasId("Practitioner/" + practitioner.getIdElement().getIdPart())))
                        .include(Condition.INCLUDE_SUBJECT)
                        .returnBundle(Bundle.class)
                        .execute();

                processPatients(bundle, emitter);

                emitter.complete();
            }));
    }

    private void processPatients(Bundle bundle, MultiEmitter<? super Patient> emitter) {
        bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof Patient)
                .map(entry -> (Patient) entry.getResource())
                .forEach(emitter::emit);

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Patient)
                    .map(entry -> (Patient) entry.getResource())
                    .forEach(emitter::emit);
        }
    }

    private Practitioner getPractitionerByIdentifier(String identifierValue) {
        Bundle bundle = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().systemAndIdentifier(PRACTITIONER_SYSTEM, identifierValue))
                .returnBundle(Bundle.class)
                .execute();
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        if (entries.isEmpty()) {
            return null;
        }
        return (Practitioner) entries.get(0).getResource();
    }

    public Multi<Practitioner> getPractitionersByName(String name) {
        return Multi.createFrom().emitter(emitter -> {
            Bundle bundle = client.search()
                    .forResource(Practitioner.class)
                    .where(Practitioner.IDENTIFIER.hasSystemWithAnyCode(PRACTITIONER_SYSTEM))
                    .where(Practitioner.NAME.contains().value(name))
                    .sort().ascending(Practitioner.NAME)
                    .returnBundle(Bundle.class)
                    .execute();

            processPractitioners(bundle, emitter);

            emitter.complete();
        });
    }

    private void processPractitioners(Bundle bundle, MultiEmitter<? super Practitioner> emitter) {
        bundle.getEntry().stream()
                .map(entry -> (Practitioner) entry.getResource())
                .forEach(emitter::emit);

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            bundle.getEntry().stream()
                    .map(entry -> (Practitioner) entry.getResource())
                    .forEach(emitter::emit);
        }
    }

    public PatientData getPatientData(Patient patient) {
        if (patient == null) {
            return null;
        }
        String ssn = "";
        if (patient.hasIdentifier()) {
            for (Identifier id : patient.getIdentifier()) {
                if (id.hasSystem() && id.getSystem().equals(PATIENT_SYSTEM)) {
                    ssn = id.getValue();
                    break;
                }
            }
        }
        String fullName = "";
        if (patient.hasName() && !patient.getName().isEmpty()) {
            fullName = patient.getNameFirstRep().getNameAsSingleString();
        }
        Enumerations.AdministrativeGender administrativeGender = patient.getGender();
        Gender gender = null;
        if (administrativeGender != null) {
            gender =
                    switch (administrativeGender) {
                        case FEMALE -> Gender.Female;
                        case MALE -> Gender.Male;
                        case UNKNOWN -> Gender.Unknown;
                        case OTHER -> Gender.Other;
                        case NULL -> null;
                    };
        }
        String email = "";
        String phone = "";
        if (patient.hasTelecom()) {
            for (ContactPoint contactPoint : patient.getTelecom()) {
                if (phone.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    phone = contactPoint.getValue();
                }
                if (email.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    email = contactPoint.getValue();
                }
            }
        }
        String line = "";
        String city = "";
        String postalCode = "";
        if (patient.hasAddress() && !patient.getAddress().isEmpty()) {
            Address address = patient.getAddressFirstRep();
            if (address.hasLine() && !address.getLine().isEmpty()) {
                line = address.getLine().get(0).getValue();
            }
            if (address.hasCity()) {
                city = address.getCity();
            }
            if (address.hasPostalCode()) {
                postalCode = address.getPostalCode();
            }
        }

        return new PatientData(ssn, fullName, gender, email, phone, line, city, postalCode);
    }

    public PractitionerData getPractitionerData(Practitioner practitioner) {
        if (practitioner == null) {
            return null;
        }

        String hsaId = "";
        for (Identifier id : practitioner.getIdentifier()) {
            if (id.getSystem().equals(PRACTITIONER_SYSTEM)) {
                hsaId = id.getValue();
                break;
            }
        }

        String phone = "";
        String email = "";
        if (practitioner.hasTelecom()) {
            for (ContactPoint contactPoint : practitioner.getTelecom()) {
                if (phone.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    phone = contactPoint.getValue();
                }
                if (email.isEmpty() && contactPoint.hasSystem() &&
                        contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    email = contactPoint.getValue();
                }
            }
        }

        String fullName = practitioner.getName().get(0).getNameAsSingleString();

        String role = "";
        List<PractitionerRole> practitionerRoles = getPractitionerRoleByPractitionerId(practitioner.getIdPart());
        if (!practitionerRoles.isEmpty()) {
            PractitionerRole practitionerRole = practitionerRoles.get(0);
            if (practitionerRole.hasCode() && !practitionerRole.getCode().isEmpty()) {
                CodeableConcept codeableConcept = practitionerRole.getCodeFirstRep();
                if (codeableConcept.hasCoding()) {
                    for (Coding coding : codeableConcept.getCoding()) {
                        if (coding.getSystem().equals(PRACTITIONER_ROLE_SYSTEM)) {
                            role = coding.getDisplay();
                        }
                    }
                }
            }
        }

        return new PractitionerData(hsaId, fullName, role, email, phone);
    }

    private List<PractitionerRole> getPractitionerRoleByPractitionerId(String id) {
        Bundle bundle = client.search()
                .forResource(PractitionerRole.class)
                .where(PractitionerRole.PRACTITIONER.hasId("Practitioner/" + id))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream().map(pR -> (PractitionerRole) pR.getResource())
                .toList();
    }
}