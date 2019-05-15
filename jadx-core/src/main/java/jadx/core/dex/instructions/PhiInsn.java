package jadx.core.dex.instructions;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class PhiInsn extends InsnNode {

	// map arguments to blocks (in same order as in arguments list)
	private final List<BlockNode> blockBinds;

	public PhiInsn(int regNum, int predecessors) {
		super(InsnType.PHI, predecessors);
		this.blockBinds = new ArrayList<>(predecessors);
		setResult(InsnArg.reg(regNum, ArgType.UNKNOWN));
		add(AFlag.DONT_INLINE);
		add(AFlag.DONT_GENERATE);
	}

	public RegisterArg bindArg(BlockNode pred) {
		RegisterArg arg = InsnArg.reg(getResult().getRegNum(), getResult().getInitType());
		bindArg(arg, pred);
		return arg;
	}

	public void bindArg(RegisterArg arg, BlockNode pred) {
		if (blockBinds.contains(pred)) {
			throw new JadxRuntimeException("Duplicate predecessors in PHI insn: " + pred + ", " + this);
		}
		super.addArg(arg);
		blockBinds.add(pred);
	}

	@Nullable
	public BlockNode getBlockByArg(RegisterArg arg) {
		int index = getArgIndex(arg);
		if (index == -1) {
			return null;
		}
		return blockBinds.get(index);
	}

	public BlockNode getBlockByArgIndex(int argIndex) {
		return blockBinds.get(argIndex);
	}

	@Override
	@NotNull
	public RegisterArg getArg(int n) {
		return (RegisterArg) super.getArg(n);
	}

	@Override
	public boolean removeArg(InsnArg arg) {
		int index = getArgIndex(arg);
		if (index == -1) {
			return false;
		}
		removeArg(index);
		return true;
	}

	@Override
	protected RegisterArg removeArg(int index) {
		RegisterArg reg = (RegisterArg) super.removeArg(index);
		blockBinds.remove(index);
		InsnRemover.fixUsedInPhiFlag(reg);
		return reg;
	}

	@Override
	public boolean replaceArg(InsnArg from, InsnArg to) {
		if (!(from instanceof RegisterArg) || !(to instanceof RegisterArg)) {
			return false;
		}

		int argIndex = getArgIndex(from);
		if (argIndex == -1) {
			return false;
		}
		BlockNode pred = getBlockByArgIndex(argIndex);
		if (pred == null) {
			throw new JadxRuntimeException("Unknown predecessor block by arg " + from + " in PHI: " + this);
		}
		removeArg(argIndex);

		RegisterArg reg = (RegisterArg) to;
		bindArg(reg, pred);
		reg.getSVar().setUsedInPhi(this);
		return true;
	}

	@Override
	public void addArg(InsnArg arg) {
		throw new JadxRuntimeException("Direct addArg is forbidden for PHI insn, bindArg must be used");
	}

	@Override
	public void setArg(int n, InsnArg arg) {
		throw new JadxRuntimeException("Direct setArg is forbidden for PHI insn, bindArg must be used");
	}

	@Override
	public String toString() {
		return "PHI: " + getResult() + " = " + Utils.listToString(getArguments())
				+ " binds: " + blockBinds;
	}
}
