package com.kinplatform.kin.knowledge;

import java.util.List;

/**
 * Puerto de fuente de conocimiento externo (ADR-014).
 *
 * <p>El dominio define el contrato; los adaptadores de infraestructura (HTTP,
 * fuentes públicas, bases de datos, RAG, documentos) lo implementan. A través
 * de este puerto el dominio nunca ve la red: solo recibe candidatos crudos que
 * Java decide validar y normalizar.</p>
 */
public interface KnowledgeSource {

    List<KnowledgeCandidate> fetch(KnowledgeQuery query);
}
