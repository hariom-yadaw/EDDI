package ai.labs.resources.impl.output.rest;

import ai.labs.persistence.IResourceStore;
import ai.labs.resources.impl.resources.rest.RestVersionInfo;
import ai.labs.resources.rest.IRestVersionInfo;
import ai.labs.resources.rest.documentdescriptor.IDocumentDescriptorStore;
import ai.labs.resources.rest.documentdescriptor.model.DocumentDescriptor;
import ai.labs.resources.rest.output.IOutputStore;
import ai.labs.resources.rest.output.IRestOutputStore;
import ai.labs.resources.rest.output.model.OutputConfigurationSet;
import ai.labs.resources.rest.patch.PatchInstruction;
import ai.labs.utilities.RestUtilities;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * @author ginccc
 */
@Slf4j
public class RestOutputStore extends RestVersionInfo<OutputConfigurationSet> implements IRestOutputStore {
    private final IOutputStore outputStore;

    @Inject
    public RestOutputStore(IOutputStore outputStore,
                           IDocumentDescriptorStore documentDescriptorStore) {
        super(resourceURI, outputStore, documentDescriptorStore);
        this.outputStore = outputStore;
    }

    @Override
    public List<DocumentDescriptor> readOutputDescriptors(String filter, Integer index, Integer limit) {
        return readDescriptors("ai.labs.output", filter, index, limit);
    }

    @Override
    public OutputConfigurationSet readOutputSet(String id, Integer version, String filter, String order, Integer index, Integer limit) {
        try {
            return outputStore.read(id, version, filter, order, index, limit);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public List<String> readOutputKeys(String id, Integer version, String filter, String order, Integer limit) {
        try {
            return outputStore.readOutputKeys(id, version, filter, order, limit);
        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public URI updateOutputSet(String id, Integer version, OutputConfigurationSet outputConfigurationSet) {
        try {
            Collections.sort(outputConfigurationSet.getOutputs(), (o1, o2) -> {
                int comparisonOfKeys = o1.getKey().compareTo(o2.getKey());
                if (comparisonOfKeys == 0) {
                    return o1.getOccurrence() < o2.getOccurrence() ? -1 : o1.getOccurrence() == o2.getOccurrence() ? 0 : 1;
                } else {
                    return comparisonOfKeys;
                }
            });

            Integer newVersion = outputStore.update(id, version, outputConfigurationSet);
            return RestUtilities.createURI(resourceURI, id, IRestVersionInfo.versionQueryParam, newVersion);
        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceModifiedException e) {
            try {
                IResourceStore.IResourceId currentId = outputStore.getCurrentResourceId(id);
                throw RestUtilities.createConflictException(resourceURI, currentId);
            } catch (IResourceStore.ResourceNotFoundException e1) {
                throw new NotFoundException(e.getLocalizedMessage(), e);
            }
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Response createOutputSet(OutputConfigurationSet outputConfigurationSet) {
        return create(outputConfigurationSet);
    }

    @Override
    public void deleteOutputSet(String id, Integer version) {
        delete(id, version);
    }

    @Override
    public URI patchOutputSet(String id, Integer version, PatchInstruction<OutputConfigurationSet>[] patchInstructions) {
        try {
            OutputConfigurationSet currentOutputConfigurationSet = outputStore.read(id, version);
            OutputConfigurationSet patchedOutputConfigurationSet = patchDocument(currentOutputConfigurationSet, patchInstructions);

            return updateOutputSet(id, version, patchedOutputConfigurationSet);

        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        }
    }

    private OutputConfigurationSet patchDocument(OutputConfigurationSet currentOutputConfigurationSet, PatchInstruction<OutputConfigurationSet>[] patchInstructions) throws IResourceStore.ResourceStoreException {
        for (PatchInstruction<OutputConfigurationSet> patchInstruction : patchInstructions) {
            OutputConfigurationSet outputConfigurationSetPatch = patchInstruction.getDocument();
            switch (patchInstruction.getOperation()) {
                case SET:
                    currentOutputConfigurationSet.getOutputs().removeAll(outputConfigurationSetPatch.getOutputs());
                    currentOutputConfigurationSet.getOutputs().addAll(outputConfigurationSetPatch.getOutputs());
                    break;
                case DELETE:
                    currentOutputConfigurationSet.getOutputs().removeAll(outputConfigurationSetPatch.getOutputs());
                    break;
                default:
                    throw new IResourceStore.ResourceStoreException("Patch operation must be either SET or DELETE!");
            }
        }

        return currentOutputConfigurationSet;
    }

    @Override
    protected IResourceStore.IResourceId getCurrentResourceId(String id) throws IResourceStore.ResourceNotFoundException {
        return outputStore.getCurrentResourceId(id);
    }
}
