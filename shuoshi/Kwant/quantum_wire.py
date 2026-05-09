#!/usr/bin/env python
# coding: utf-8

# In[1]:


# Tutorial 2.2.2. Transport through a quantum wire
# ================================================
#
# Physics background
# ------------------
#  Conductance of a quantum wire; subbands
#
# Kwant features highlighted
# --------------------------
#  - Builder for setting up transport systems easily
#  - Making scattering region and leads
#  - Using the simple sparse solver for computing Landauer conductance

# For plotting
from matplotlib import pyplot


# In[2]:


import matplotlib
import matplotlib.pyplot
from matplotlib_inline.backend_inline import set_matplotlib_formats

matplotlib.rcParams['figure.figsize'] = matplotlib.pyplot.figaspect(1) * 2
set_matplotlib_formats('svg')


# In[3]:


import kwant


# In[4]:


# First define the tight-binding system

syst = kwant.Builder()


# In[5]:


a = 1
lat = kwant.lattice.square(a, norbs=1)


# In[6]:


t = 1.0
W, L = 10, 30

# Define the scattering region

for i in range(L):
    for j in range(W):
        # On-site Hamiltonian
        syst[lat(i, j)] = 4 * t

        # Hopping in y-direction
        if j > 0:
            syst[lat(i, j), lat(i, j - 1)] = -t

        # Hopping in x-direction
        if i > 0:
            syst[lat(i, j), lat(i - 1, j)] = -t


# In[7]:


# Then, define and attach the leads:

# First the lead to the left
# (Note: TranslationalSymmetry takes a real-space vector)
sym_left_lead = kwant.TranslationalSymmetry((-a, 0))
left_lead = kwant.Builder(sym_left_lead)


# In[8]:


for j in range(W):
    left_lead[lat(0, j)] = 4 * t
    if j > 0:
        left_lead[lat(0, j), lat(0, j - 1)] = -t
    left_lead[lat(1, j), lat(0, j)] = -t


# In[9]:


syst.attach_lead(left_lead)


# In[10]:


# Then the lead to the right

sym_right_lead = kwant.TranslationalSymmetry((a, 0))
right_lead = kwant.Builder(sym_right_lead)

for j in range(W):
    right_lead[lat(0, j)] = 4 * t
    if j > 0:
        right_lead[lat(0, j), lat(0, j - 1)] = -t
    right_lead[lat(1, j), lat(0, j)] = -t

syst.attach_lead(right_lead)


# In[11]:


kwant.plot(syst);


# In[12]:


# Finalize the system
syst = syst.finalized()


# In[13]:


# Now that we have the system, we can compute conductance
energies = []
data = []
for ie in range(100):
    energy = ie * 0.01

    # compute the scattering matrix at a given energy
    smatrix = kwant.smatrix(syst, energy)

    # compute the transmission probability from lead 0 to
    # lead 1
    energies.append(energy)
    data.append(smatrix.transmission(1, 0))


# In[14]:


# Use matplotlib to write output
# We should see conductance steps
pyplot.figure()
pyplot.plot(energies, data)
pyplot.xlabel("energy [t]")
pyplot.ylabel("conductance [e^2/h]")
pyplot.show()

