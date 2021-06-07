package ca.voidstarzero.soliddemo.service

import org.apache.commons.io.IOUtils
import org.apache.jena.rdf.model.ModelFactory
import org.springframework.stereotype.Service

@Service
class GraphUtils
{
    fun graph(ttlResponse: String, baseUri: String): org.apache.jena.rdf.model.Model
    {
        return ModelFactory.createDefaultModel()
            .read(
                IOUtils.toInputStream(ttlResponse, "UTF-8"),
                baseUri,
                "TURTLE"
            )
    }
}