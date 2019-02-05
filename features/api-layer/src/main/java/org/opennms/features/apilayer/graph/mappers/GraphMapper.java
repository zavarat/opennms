/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.apilayer.graph.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.opennms.integration.api.v1.graph.beans.ImmutableVertexRef;
import org.opennms.netmgt.graph.api.VertexRef;
import org.opennms.netmgt.graph.api.generic.GenericEdge;
import org.opennms.netmgt.graph.api.generic.GenericGraph;
import org.opennms.netmgt.graph.api.generic.GenericProperties;
import org.opennms.netmgt.graph.api.generic.GenericVertex;

@Mapper(collectionMappingStrategy=CollectionMappingStrategy.SETTER_PREFERRED)
public interface GraphMapper {

    org.opennms.integration.api.v1.graph.beans.ImmutableGraph map(GenericGraph graph);

    GenericGraph map(org.opennms.integration.api.v1.graph.Graph graph);

    @AfterMapping
    default void fillGraph(org.opennms.integration.api.v1.graph.Graph graph, @MappingTarget GenericGraph genericGraph) {
        // There's not setNamespace property on the generic graph, so we do this manually
        genericGraph.setProperty(GenericProperties.NAMESPACE, graph.getNamespace());
    }

    default org.opennms.integration.api.v1.graph.Vertex toApiVertex(org.opennms.netmgt.graph.api.generic.GenericVertex vertex) {
        return org.opennms.integration.api.v1.graph.beans.ImmutableVertex.builder()
                .namespace(vertex.getNamespace())
                .id(vertex.getId())
                .build();
    }

    default GenericVertex toGenericVertex(org.opennms.integration.api.v1.graph.Vertex vertex) {
        return new GenericVertex(vertex.getNamespace(), vertex.getId());
    }

    default GenericVertexRef toGenericVertexRef(org.opennms.integration.api.v1.graph.VertexRef vertexRef) {
        return new GenericVertexRef(vertexRef);
    }

    default ImmutableVertexRef toApiVertexRef(VertexRef vertexRef) {
        return org.opennms.integration.api.v1.graph.beans.ImmutableVertexRef.builder()
                .namespace(vertexRef.getNamespace())
                .id(vertexRef.getId())
                .build();
    }

    default org.opennms.integration.api.v1.graph.Edge toApiEdge(org.opennms.netmgt.graph.api.generic.GenericEdge edge) {
        return org.opennms.integration.api.v1.graph.beans.ImmutableEdge.builder()
                .namespace(edge.getNamespace())
                .id(edge.getId())
                .source(toApiVertexRef(edge.getSource()))
                .target(toApiVertexRef(edge.getTarget()))
                .build();
    }

    default GenericEdge toGenericEdge(org.opennms.integration.api.v1.graph.Edge edge) {
        return new GenericEdge(toGenericVertexRef(edge.getSource()), toGenericVertexRef(edge.getTarget()));
    }
}
