# Makefile for canvas classes and packages-Ptolemy II specific version of Diva
#
# @Version: $Id$
# @Authors: Christopher Hylands (makefile only)
#
# @Copyright (c) 2003-2009 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY

ME =		diva/canvas

DIRS = 		connector demo event interactor toolbox test tutorial

# Root of the Ptolemy II directory
ROOT =		../..

CLASSPATH =	$(ROOT)

# Get configuration info
CONFIG =	$(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE = 	canvas
PTCLASSJAR =

# Include the .class files from these jars in PTCLASSALLJAR
PTCLASSALLJARS = \
	connector/connector.jar \
	demo/demo.jar \
	event/event.jar \
	interactor/interactor.jar \
	toolbox/toolbox.jar

PTCLASSALLJAR = $(PTPACKAGE).jar

JSRCS = \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	AbstractSite.java \
	BasicZList.java \
	CanvasComponent.java \
	CanvasDebugUtilities.java \
	CanvasLayer.java \
	CanvasPane.java \
	CanvasUtilities.java \
	CompositeFigure.java \
	DamageRegion.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureLayer.java \
	FigureSet.java \
	FigureWrapper.java \
	GeometricSet.java \
	GraphicsPane.java \
	JCanvas.java \
	OverlayLayer.java \
	PaneWrapper.java \
	Site.java \
	SiteDecorator.java \
	TransformContext.java \
	VisibleComponent.java \
	ZList.java

EXTRA_SRCS =	$(JSRCS) \
	package.html

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
# Don't include demo or DIRS here, or else 'make sources' will run 'make demo'
MISC_FILES =	connector event interactor toolbox test tutorial

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = \
	doc \
	demo \
	$(PTCLASSJAR) \
	$(PTCLASSALLJAR) \
	'BasicZList$$1.class' \
	'BasicZList$$2.class' \
	'BasicZList$$EnclosedGSet.class' \
	'BasicZList$$GSet.class' \
	'BasicZList$$GSet$$1.class' \
	'BasicZList$$IntersectedGSet.class' \
	'CanvasUtilities$$1.class' \
	'DamageRegion$$1.class' \
	'DamageRegion$$RectangleDamageRegion.class' \
	'GraphicsPane$$1.class' \
	'GraphicsPane$$2.class'

JCLASS = $(JSRCS:%.java=%.class)

all: jclass
# Use subinstall here so that we create tutorial/doc-file
install: jclass subinstall jars

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
