package com.uber;

import javax.annotation.Nullable;

public interface SSAInstructionFactory {

    SSAAbstractInvokeInstruction InvokeInstruction(int index, int result, int[] params, int exception, CallSiteReference site, @Nullable BootstrapMethod bootstrap);

    SSAAbstractInvokeInstruction InvokeInstruction(int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);
}
