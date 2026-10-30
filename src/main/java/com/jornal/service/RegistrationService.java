package com.jornal.service;

import com.jornal.dto.form.ForemanRegistrationForm;
import com.jornal.dto.form.WorkerRegistrationForm;
import java.util.UUID;

/** Alta de cuentas. Es la unica operacion de negocio disponible sin iniciar sesion. */
public interface RegistrationService {

    UUID registerForeman(ForemanRegistrationForm form);

    UUID registerWorker(WorkerRegistrationForm form);
}
