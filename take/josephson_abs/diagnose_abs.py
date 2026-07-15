import kwant
import numpy as np
import matplotlib.pyplot as plt
from scipy.linalg import eig
from tqdm import tqdm
import concurrent.futures

# ===== 复制 notebook 的核心函数 =====
sx = np.array([[0, 1], [1, 0]], complex)
sy = np.array([[0, -1j], [1j, 0]], complex)
sz = np.array([[1, 0], [0, -1]], complex)
s0 = np.array([[1, 0], [0, 1]], complex)

def H_onsite(weineng, mu, Delta, chaodaojiao):
    Delta_X = Delta * np.exp(1j*chaodaojiao) * 1j * sy
    HX_block = (weineng - mu) * s0
    return np.block([
        [HX_block, Delta_X],
        [Delta_X.conj().T, -HX_block.conj()]
    ])

def H_hop(t):
    H_X_L_to_R_hop_block = -t * s0
    return np.block([
        [H_X_L_to_R_hop_block, np.zeros((2,2))],
        [np.zeros((2,2)), -H_X_L_to_R_hop_block.conj()]
    ])

def build_full_Hamiltonian(H_L_onsite_list, H_L_left_to_right_hop, 
                           H_center_left_to_right_hop, 
                           H_R_onsite_list, H_R_left_to_right_hop):
    N_L = len(H_L_onsite_list)
    N_R = len(H_R_onsite_list)
    N_total = N_L + N_R
    dim = 4
    H_full = np.zeros((N_total * dim, N_total * dim), dtype=complex)
    H_onsite_all = H_L_onsite_list + H_R_onsite_list
    for i in range(N_total):
        rs, re = i * dim, (i + 1) * dim
        H_full[rs:re, rs:re] = H_onsite_all[i]
    for i in range(N_total - 1):
        if i < N_L - 1:
            hop = H_L_left_to_right_hop
        elif i == N_L - 1:
            hop = H_center_left_to_right_hop
        else:
            hop = H_R_left_to_right_hop
        rs1, re1 = i * dim, (i + 1) * dim
        rs2, re2 = (i + 1) * dim, (i + 2) * dim
        H_full[rs1:re1, rs2:re2] = hop
        H_full[rs2:re2, rs1:re1] = hop.conj().T
    return H_full

# ===== 参数 =====
t = -1
dela = 0.1 * t
Delta = dela
mu = 0
weineng_a = -1.1 * t  # = 1.1
weineng_b = 0.7 * t   # = -0.7
tc = 0.6 * t  # = -0.6
omega = 89 / 144

# Fibonacci sequences
def fib_seq(N, omega, phase_shift):
    seq = []
    for j in range(1, N + 1):
        X_j = np.sign(np.cos(2 * np.pi * j * omega + phase_shift) - np.cos(np.pi * omega))
        seq.append(weineng_a if X_j >= 0 else weineng_b)
    return seq

yigejiao_L = 0.9 * np.pi
yigejiao_R = 0.45 * np.pi
N_L = 233
N_R = 233

weineng_L_list = fib_seq(N_L, omega, yigejiao_L)
weineng_R_list = fib_seq(N_R, omega, yigejiao_R)

H_L_hop = H_hop(t)
H_center_hop = H_hop(tc)
H_R_hop = H_hop(t)

# ===== 密集扫描 phi，过滤 gap 内状态 =====
phases = np.linspace(0, 2 * np.pi, 60)
gap_states = []  # states with |E| < |Delta|

print(f"Delta = {Delta:.4f}, gap edge = {abs(Delta):.4f}")
print(f"Total system sites: {N_L+N_R}, matrix size: {(N_L+N_R)*4}")

H_R_onsite_list = [H_onsite(w, mu, Delta, 0) for w in weineng_R_list]

def compute_gap_states(phi):
    H_L = [H_onsite(w, mu, Delta, phi) for w in weineng_L_list]
    H_full = build_full_Hamiltonian(H_L, H_L_hop, H_center_hop, H_R_onsite_list, H_R_hop)
    evals = np.linalg.eigvalsh(H_full)
    gap_mask = np.abs(evals) < abs(Delta)
    gap_evals = evals[gap_mask]
    return phi, gap_evals, len(gap_evals)

print("\n扫描 phi 找 gap 内状态...")
gap_edge = abs(Delta)

with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
    futures = {executor.submit(compute_gap_states, phi): phi for phi in phases}
    for future in tqdm(concurrent.futures.as_completed(futures), total=len(phases)):
        phi, evals, n_gap = future.result()
        if n_gap > 0:
            gap_states.append((phi / np.pi, evals / abs(Delta)))
            print(f"  φ/π = {phi/np.pi:.3f}: found {n_gap} gap states at E/Δ = {evals/abs(Delta)}")

if len(gap_states) == 0:
    print("\n*** 没找到任何 |E| < |Δ| 的状态！***")
    print("\n=== 原因诊断 ===")
    # 检查一个典型 phi 的能谱
    phi_test = 0.5 * np.pi
    H_L_test = [H_onsite(w, mu, Delta, phi_test) for w in weineng_L_list]
    H_full_test = build_full_Hamiltonian(H_L_test, H_L_hop, H_center_hop, H_R_onsite_list, H_R_hop)
    evals_test = np.linalg.eigvalsh(H_full_test)
    print(f"\n测试 phi = {phi_test/np.pi:.2f}π:")
    print(f"  最小 |E| = {np.min(np.abs(evals_test)):.6f} (gap edge = {gap_edge:.4f})")
    print(f"  最小 20 个 |E|: {np.sort(np.abs(evals_test))[:20]}")
    print(f"  能谱范围: [{np.min(evals_test):.4f}, {np.max(evals_test):.4f}]")
    
    # 检查单个格点的超导哈密顿量能隙
    H_L_0 = H_onsite(weineng_L_list[0], mu, Delta, 0)
    evals_single = np.linalg.eigvalsh(H_L_0)
    print(f"\n单个超导格点本征值: {evals_single}")
    print(f"  预期间隙: ±{abs(Delta):.4f}")
    
    # 检查纯超导体（无结）的能隙
    print("\n=== 检查纯 Fibonacci 超导体的能隙 ===")
    N_test = 100
    w_list = fib_seq(N_test, omega, 0)
    H_test_SC = [H_onsite(w, mu, Delta, 0) for w in w_list]
    blocks = [[np.zeros((4,4), complex) for _ in range(N_test)] for _ in range(N_test)]
    for i in range(N_test):
        blocks[i][i] = H_test_SC[i]
    for i in range(N_test-1):
        hop = H_L_hop
        blocks[i][i+1] = hop
        blocks[i+1][i] = hop.conj().T
    H_pure_SC = np.block(blocks)
    evals_SC = np.linalg.eigvalsh(H_pure_SC)
    gap_SC = np.min(np.abs(evals_SC))
    print(f"  纯 Fibonacci 超导体 ({N_test} sites): 最小 |E| = {gap_SC:.6f}")
    print(f"  预期间隙 = {abs(Delta):.6f}")
    if gap_SC < 0.001:
        print("  *** 纯 Fibonacci 超导体也没有能隙！Fibonacci 势关闭了超导能隙 ***")
    else:
        print(f"  纯超导体有能隙 OK (约占预期的 {gap_SC/abs(Delta)*100:.1f}%)")
else:
    print(f"\n找到 {len(gap_states)} 个 phase 点有 gap 内状态")
    # Plot ABS
    fig, ax = plt.subplots(figsize=(8, 5))
    for phi_vals, e_vals in gap_states:
        ax.scatter([phi_vals]*len(e_vals), e_vals, s=15, c='red')
    ax.axhline(1, color='gray', ls='--', alpha=0.5)
    ax.axhline(-1, color='gray', ls='--', alpha=0.5)
    ax.axhline(0, color='gray', ls='-', alpha=0.3)
    ax.set_xlabel(r'$\phi / \pi$', fontsize=14)
    ax.set_ylabel(r'$E / \Delta$', fontsize=14)
    ax.set_title('Andreev Bound States (gap-filtered)', fontsize=14)
    ax.set_xlim(0, 2)
    ax.set_ylim(-1.5, 1.5)
    plt.tight_layout()
    plt.savefig('/c/Users/taoji/code/take/josephson_abs/abs_diagnosis.png', dpi=150)
    print("Plot saved to abs_diagnosis.png")

