package net.vulkanmod.vulkan.shader.descriptor;

public class descriptorArray<T> {

    final T[] imageDescriptor;

    final int maxSize;
    final int shaderStage;

    final int descriptorType;
    final int descriptorBinding;
    private int maxOffset; //Max mspaler ofoset: i.e./ the max size/.range of this descriptor Arrau

    /*Abtracts betwen OpenGl texure Bindings and and Initialised desctior indicies for this particualr dEscriptir Binding*/

//    public descriptorAbstractionTable(int maxSize, int shaderStage, int descriptorType, int descriptorBinding) {
    public descriptorArray(int maxSize, int shaderStage, int descriptorType, int descriptorBinding) {
        this.maxSize = maxSize;
        this.shaderStage = shaderStage;
        this.descriptorType = descriptorType;
        this.imageDescriptor = (T[])(new Object[maxSize]);

        this.descriptorBinding = descriptorBinding;
    }

    //Initialise a specific descriptor handle
    public void initialiseDescriptorIndex(int idx, long handl)
    {

    }


    public T getInitSampler(int idx)
    {
        return this.imageDescriptor[idx];
    }

    public T getInitSampler2Pipeline(String idx, int stage)
    {
        return this.imageDescriptor[idx.hashCode()];
    }


    public void addSampler(T sampler) {
        this.imageDescriptor[maxOffset++]= sampler;
    }
}
