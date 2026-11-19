package com.radiocare.service.conflit;

import java.util.List;

/** Une règle de contrôle du moteur de conflits (patron Stratégie / Chaîne de règles). */
public interface RegleConflit {
    List<Conflit> verifier(ContexteConflit ctx);
}
